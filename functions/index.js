const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const stream = require("stream");

admin.initializeApp();

const DRIVE_ROOT_FOLDER_ID = "1bgIXv6zTpd9oT8Fr3QwCtIaZ1eCDwKtc";

// In-memory cache for folder IDs during function execution
const folderCache = new Map();

function getDriveClient() {
  const auth = new google.auth.GoogleAuth({
    scopes: [
      "https://www.googleapis.com/auth/drive",
      "https://www.googleapis.com/auth/drive.file"
    ],
  });
  return google.drive({ version: "v3", auth });
}

function getCategoryFolderName(categoryId) {
  const cat = (categoryId || "").toLowerCase().trim();
  switch (cat) {
    case "school_photo":
      return "01 School Photo";
    case "explaining_app":
      return "02 Explaining App";
    case "students_smart_board":
    case "smart_board":
      return "03 Smart Board";
    case "principal_photo":
    case "principal":
      return "04 Principal";
    case "letter_photo":
    case "letter":
      return "05 Letter";
    case "other_photos":
    default:
      return "06 Other Photos";
  }
}

function getStandardFileName(categoryId, originalFileName, index = 1) {
  const cat = (categoryId || "").toLowerCase().trim();
  const ext = (originalFileName && originalFileName.includes(".")) 
    ? originalFileName.substring(originalFileName.lastIndexOf(".")) 
    : ".jpg";

  switch (cat) {
    case "school_photo":
      return `01_School_Photo${ext}`;
    case "explaining_app":
      return `02_Explaining_App${ext}`;
    case "students_smart_board":
    case "smart_board":
      return `03_Smart_Board${ext}`;
    case "principal_photo":
    case "principal":
      return `04_Principal${ext}`;
    case "letter_photo":
    case "letter":
      return `05_Letter${ext}`;
    case "other_photos":
    default:
      return `06_Other_Media_${index}${ext}`;
  }
}

async function getOrCreateFolder(drive, parentId, folderName) {
  const cleanName = (folderName || "").trim().replace(/[\\/:*?"<>|]/g, "_") || "Unknown";
  const cacheKey = `${parentId}:::${cleanName}`;
  if (folderCache.has(cacheKey)) {
    return folderCache.get(cacheKey);
  }

  const safeQueryName = cleanName.replace(/'/g, "\\'");
  const query = `mimeType = 'application/vnd.google-apps.folder' and name = '${safeQueryName}' and '${parentId}' in parents and trashed = false`;

  try {
    const listRes = await drive.files.list({
      q: query,
      fields: "files(id, name)",
      spaces: "drive",
      pageSize: 1,
      supportsAllDrives: true,
      includeItemsFromAllDrives: true
    });

    if (listRes.data.files && listRes.data.files.length > 0) {
      const existingId = listRes.data.files[0].id;
      folderCache.set(cacheKey, existingId);
      return existingId;
    }
  } catch (err) {
    console.warn(`Search folder '${cleanName}' in parent '${parentId}' warning:`, err.message);
  }

  const createRes = await drive.files.create({
    requestBody: {
      name: cleanName,
      mimeType: "application/vnd.google-apps.folder",
      parents: [parentId]
    },
    fields: "id, name",
    supportsAllDrives: true
  });

  const createdId = createRes.data.id;
  folderCache.set(cacheKey, createdId);
  return createdId;
}

/**
 * Builds the complete hierarchy:
 * Root -> SOE APP DATA -> Year -> State -> District -> Block -> School (UDISE) -> Visit Date -> Category
 */
async function resolveCategoryFolderId(drive, params) {
  const {
    year,
    state,
    district,
    block,
    schoolName,
    udiseCode,
    visitDate,
    categoryId
  } = params;

  // 1. Root -> SOE APP DATA
  const soeAppDataId = await getOrCreateFolder(drive, DRIVE_ROOT_FOLDER_ID, "SOE APP DATA");

  // 2. Year (e.g. "2026")
  let effectiveYear = (year || "").trim();
  if (!effectiveYear) {
    const match = (visitDate || "").match(/\b(20\d\d)\b/);
    effectiveYear = match ? match[1] : new Date().getFullYear().toString();
  }
  const yearFolderId = await getOrCreateFolder(drive, soeAppDataId, effectiveYear);

  // 3. State (e.g. "Rajasthan")
  const effectiveState = (state || "Rajasthan").trim() || "Rajasthan";
  const stateFolderId = await getOrCreateFolder(drive, yearFolderId, effectiveState);

  // 4. District (e.g. "Jaipur")
  const effectiveDistrict = (district || "Unknown District").trim() || "Unknown District";
  const districtFolderId = await getOrCreateFolder(drive, stateFolderId, effectiveDistrict);

  // 5. Block (e.g. "Amber")
  const effectiveBlock = (block || "Unknown Block").trim() || "Unknown Block";
  const blockFolderId = await getOrCreateFolder(drive, districtFolderId, effectiveBlock);

  // 6. School Name (UDISE Code)
  const cleanSchool = (schoolName || "Unknown School").trim() || "Unknown School";
  const cleanUdise = (udiseCode || "").trim();
  const schoolFolderName = cleanUdise && !cleanSchool.includes(cleanUdise)
    ? `${cleanSchool} (${cleanUdise})`
    : cleanSchool;
  const schoolFolderId = await getOrCreateFolder(drive, blockFolderId, schoolFolderName);

  // 7. Visit Date
  const effectiveDate = (visitDate || new Date().toISOString().split("T")[0]).trim();
  const visitDateFolderId = await getOrCreateFolder(drive, schoolFolderId, effectiveDate);

  // 8. Category Folder
  const categoryFolderName = getCategoryFolderName(categoryId);
  const categoryFolderId = await getOrCreateFolder(drive, visitDateFolderId, categoryFolderName);

  return {
    targetFolderId: categoryFolderId,
    path: `SOE APP DATA/${effectiveYear}/${effectiveState}/${effectiveDistrict}/${effectiveBlock}/${schoolFolderName}/${effectiveDate}/${categoryFolderName}`
  };
}

/**
 * Callable Cloud Function: uploadPhotoToDrive
 * Uploads a single photo/media file to Google Drive under the structured folder hierarchy.
 */
exports.uploadPhotoToDrive = functions
  .runWith({ timeoutSeconds: 300, memory: "512MB" })
  .https.onCall(async (data, context) => {
    // 1. Verify Authentication
    if (!context.auth || !context.auth.uid) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "You must be authenticated to upload visit media."
      );
    }

    const {
      base64Data,
      mimeType,
      fileName,
      categoryId,
      schoolName,
      udiseCode,
      state,
      district,
      block,
      visitDate,
      year,
      index
    } = data;

    if (!base64Data) {
      throw new functions.https.HttpsError("invalid-argument", "Missing media base64Data.");
    }

    const drive = getDriveClient();

    try {
      const { targetFolderId, path } = await resolveCategoryFolderId(drive, {
        year,
        state,
        district,
        block,
        schoolName,
        udiseCode,
        visitDate,
        categoryId
      });

      const effectiveFileName = getStandardFileName(categoryId, fileName, index || 1);
      const fileBuffer = Buffer.from(base64Data, "base64");
      const bufferStream = new stream.PassThrough();
      bufferStream.end(fileBuffer);

      const createRes = await drive.files.create({
        requestBody: {
          name: effectiveFileName,
          parents: [targetFolderId]
        },
        media: {
          mimeType: mimeType || "image/jpeg",
          body: bufferStream
        },
        fields: "id, name, webViewLink, webContentLink, size, mimeType",
        supportsAllDrives: true
      });

      const fileId = createRes.data.id;

      // Set public reader permission so Android Coil and web viewers can load the media
      try {
        await drive.permissions.create({
          fileId: fileId,
          requestBody: {
            role: "reader",
            type: "anyone"
          },
          supportsAllDrives: true
        });
      } catch (permErr) {
        console.warn("Could not set reader permission:", permErr.message);
      }

      // High performance direct URL for Coil / Image loaders
      const directUrl = `https://lh3.googleusercontent.com/d/${fileId}`;
      const viewUrl = createRes.data.webViewLink || `https://drive.google.com/file/d/${fileId}/view`;

      return {
        success: true,
        fileId: fileId,
        fileName: effectiveFileName,
        url: directUrl,
        directUrl: directUrl,
        webViewLink: viewUrl,
        webContentLink: createRes.data.webContentLink || `https://drive.google.com/uc?export=download&id=${fileId}`,
        folderId: targetFolderId,
        drivePath: path
      };
    } catch (err) {
      console.error("Failed to upload photo to Google Drive:", err);
      throw new functions.https.HttpsError("internal", err.message || "Failed to upload photo to Google Drive.");
    }
  });

/**
 * Callable Cloud Function: createEmployeeUser
 * 
 * Securely creates a Firebase Authentication user account and corresponding Firestore
 * user document (role: "EMPLOYEE", status: "ACTIVE") on behalf of an authenticated Admin.
 * 
 * Security & Data Rules:
 * 1. Caller must be authenticated with Firebase Auth.
 * 2. Caller must have role == "ADMIN" in Firestore `users/{callerUID}`.
 * 3. Checks for duplicate email in Firebase Auth and Firestore.
 * 4. Passwords are NEVER stored in Firestore or database logs.
 * 5. Uses Firebase Admin SDK to create the user account in Firebase Auth.
 * 6. Admin session is completely unaffected and remains logged in.
 */
exports.createEmployeeUser = functions.https.onCall(async (data, context) => {
  // 1. Verify Authentication
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated."
    );
  }

  const callerUid = context.auth.uid;
  const db = admin.firestore();

  // 2. Verify Caller is an ADMIN in Firestore
  let callerRole = "";
  try {
    const callerDoc = await db.collection("users").doc(callerUid).get();
    if (callerDoc.exists) {
      callerRole = (callerDoc.data().role || "").toUpperCase();
    }
  } catch (err) {
    console.error("Error reading caller profile:", err);
  }

  if (callerRole !== "ADMIN") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only Administrators can create employee accounts."
    );
  }

  // 3. Extract & Validate Input Data
  const name = (data.name || "").trim();
  const email = (data.email || "").trim().toLowerCase();
  const mobile = (data.mobile || "").trim();
  const state = (data.state || "Rajasthan").trim();
  const district = (data.district || "").trim();
  const rawPassword = (data.password || "").trim();

  if (!name) {
    throw new functions.https.HttpsError("invalid-argument", "Please enter the officer's full name.");
  }
  if (!email || !email.includes("@")) {
    throw new functions.https.HttpsError("invalid-argument", "Please enter a valid email address.");
  }

  // 4. Check for duplicate email in Firebase Authentication
  try {
    const existingUser = await admin.auth().getUserByEmail(email);
    if (existingUser) {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
  } catch (error) {
    if (error.code === "auth/email-already-exists" || error.code === "already-exists") {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
    if (error.code !== "auth/user-not-found" && !(error instanceof functions.https.HttpsError)) {
      console.warn("getUserByEmail check:", error.message);
    }
  }

  // Check Firestore for duplicate email
  try {
    const existingFirestoreDocs = await db.collection("users").where("email", "==", email).limit(1).get();
    if (!existingFirestoreDocs.empty) {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
  } catch (error) {
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
  }

  // 5. Create Firebase Authentication user with Admin SDK
  const userCreatePayload = {
    email: email,
    displayName: name,
    disabled: false
  };

  // If password provided and >= 6 characters, set initial password in Auth ONLY (never in Firestore)
  if (rawPassword && rawPassword.length >= 6) {
    userCreatePayload.password = rawPassword;
  }

  let newUserRecord;
  try {
    newUserRecord = await admin.auth().createUser(userCreatePayload);
  } catch (authError) {
    console.error("Firebase Admin createUser failed:", authError);
    if (authError.code === "auth/email-already-exists" || authError.message?.includes("already exists")) {
      throw new functions.https.HttpsError(
        "already-exists",
        "An account with this email already exists."
      );
    }
    throw new functions.https.HttpsError(
      "internal",
      authError.message || "Failed to create Firebase Authentication user."
    );
  }

  const newUid = newUserRecord.uid;

  // 6. Create Firestore Document users/{newEmployeeUID}
  try {
    await db.collection("users").doc(newUid).set({
      userId: newUid,
      name: name,
      email: email,
      mobile: mobile,
      state: state || "Rajasthan",
      district: district,
      role: "EMPLOYEE",
      status: "ACTIVE",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      createdBy: callerUid
    });
  } catch (firestoreError) {
    console.error("Firestore document write failed, rolling back auth account:", firestoreError);
    try {
      await admin.auth().deleteUser(newUid);
    } catch (delError) {
      console.error("Rollback failed for user:", newUid, delError);
    }
    throw new functions.https.HttpsError(
      "internal",
      "Failed to initialize Firestore user document."
    );
  }

  return {
    success: true,
    userId: newUid,
    name: name,
    email: email,
    role: "EMPLOYEE",
    status: "ACTIVE"
  };
});
