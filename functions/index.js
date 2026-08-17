const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const https = require("https");

admin.initializeApp();

const getCloudinaryConfig = () => {
  const cfg = (functions.config && typeof functions.config === "function" && functions.config().cloudinary) || {};
  return {
    cloudName: process.env.CLOUDINARY_CLOUD_NAME || cfg.cloud_name || "",
    apiKey: process.env.CLOUDINARY_API_KEY || cfg.api_key || "",
    apiSecret: process.env.CLOUDINARY_API_SECRET || cfg.api_secret || ""
  };
};

// Confirms the caller is Admin OR owns the given visit.
async function assertCanAccessVisit(uid, visitId) {
  if (!uid) {
    throw new functions.https.HttpsError("unauthenticated", "Sign-in required.");
  }
  const userSnap = await admin.firestore().collection("users").doc(uid).get();
  const role = userSnap.exists ? userSnap.get("role") : null;
  if (role === "ADMIN") return;

  const visitSnap = await admin.firestore().collection("visits").doc(visitId).get();
  if (!visitSnap.exists || visitSnap.get("employeeId") !== uid) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "You do not have access to this visit."
    );
  }
}

// Returns a short-lived signed-upload payload for a specific visit/school/category.
exports.getCloudinarySignature = functions.https.onCall(async (data, context) => {
  const uid = context.auth && context.auth.uid;
  const { visitId, schoolId, category, mediaId, publicId: reqPublicId, mediaIndex } = data || {};
  if (!visitId || !schoolId || !category) {
    throw new functions.https.HttpsError("invalid-argument", "visitId, schoolId, category are required.");
  }
  await assertCanAccessVisit(uid, visitId);

  const { cloudName, apiKey, apiSecret } = getCloudinaryConfig();
  if (!cloudName || !apiKey || !apiSecret) {
    throw new functions.https.HttpsError("failed-precondition", "Cloudinary credentials not configured on server.");
  }

  const timestamp = Math.floor(Date.now() / 1000);
  const folder = `visits/${visitId}/${schoolId}/${category}`;
  const cleanIndex = (typeof mediaIndex === 'number' && mediaIndex >= 0) ? mediaIndex : 0;
  const publicId = mediaId || reqPublicId || `${visitId}_${category}_${cleanIndex}`;

  // Only these params may be part of the signed request — must match exactly
  // what the Android client sends alongside the signature.
  const paramsToSign = { folder, public_id: publicId, timestamp };
  const sortedString = Object.keys(paramsToSign)
    .sort()
    .map((k) => `${k}=${paramsToSign[k]}`)
    .join("&");
  const signature = crypto
    .createHash("sha1")
    .update(sortedString + apiSecret)
    .digest("hex");

  return {
    cloudName,
    apiKey,
    timestamp,
    folder,
    publicId,
    signature,
  };
});

// Checks if a Cloudinary asset already exists (retry/lost response safety).
exports.checkCloudinaryAsset = functions.https.onCall(async (data, context) => {
  const uid = context.auth && context.auth.uid;
  const { visitId, schoolId, category, mediaId, publicId: reqPublicId, resourceType } = data || {};
  if (!visitId || !category) {
    throw new functions.https.HttpsError("invalid-argument", "visitId and category are required.");
  }
  await assertCanAccessVisit(uid, visitId);

  const { cloudName, apiKey, apiSecret } = getCloudinaryConfig();
  if (!cloudName || !apiKey || !apiSecret) {
    throw new functions.https.HttpsError("failed-precondition", "Cloudinary credentials not configured on server.");
  }

  const type = resourceType === "video" ? "video" : "image";
  const publicId = mediaId || reqPublicId || `${visitId}_${category}_0`;
  const folder = schoolId ? `visits/${visitId}/${schoolId}/${category}` : `visits/${visitId}/${category}`;
  const fullPublicId = `${folder}/${publicId}`;

  const authString = Buffer.from(`${apiKey}:${apiSecret}`).toString("base64");

  const options = {
    hostname: "api.cloudinary.com",
    path: `/v1_1/${cloudName}/resources/${type}/upload/${encodeURIComponent(fullPublicId)}`,
    method: "GET",
    headers: {
      "Authorization": `Basic ${authString}`
    }
  };

  return new Promise((resolve) => {
    const req = https.request(options, (res) => {
      let responseBody = "";
      res.on("data", (chunk) => { responseBody += chunk; });
      res.on("end", () => {
        if (res.statusCode === 200) {
          try {
            const json = JSON.parse(responseBody);
            resolve({
              exists: true,
              secureUrl: json.secure_url,
              publicId: publicId,
              folder: folder
            });
          } catch (e) {
            resolve({ exists: false });
          }
        } else {
          resolve({ exists: false });
        }
      });
    });
    req.on("error", () => resolve({ exists: false }));
    req.end();
  });
});

// Permanently deletes a Cloudinary asset after verifying ownership.
exports.deleteCloudinaryAsset = functions.https.onCall(async (data, context) => {
  const uid = context.auth && context.auth.uid;
  const { visitId, publicId, resourceType } = data || {};
  if (!visitId || !publicId) {
    throw new functions.https.HttpsError("invalid-argument", "visitId and publicId are required.");
  }
  await assertCanAccessVisit(uid, visitId);

  const { cloudName, apiKey, apiSecret } = getCloudinaryConfig();
  if (!cloudName || !apiKey || !apiSecret) {
    throw new functions.https.HttpsError("failed-precondition", "Cloudinary credentials not configured on server.");
  }

  const timestamp = Math.floor(Date.now() / 1000);
  const paramsToSign = { public_id: publicId, timestamp };
  const sortedString = Object.keys(paramsToSign)
    .sort()
    .map((k) => `${k}=${paramsToSign[k]}`)
    .join("&");
  const signature = crypto
    .createHash("sha1")
    .update(sortedString + apiSecret)
    .digest("hex");

  const type = resourceType === "video" ? "video" : "image";
  const body = new URLSearchParams({
    public_id: publicId,
    timestamp: String(timestamp),
    api_key: apiKey,
    signature,
  }).toString();

  await new Promise((resolve, reject) => {
    const req = https.request(
      {
        hostname: "api.cloudinary.com",
        path: `/v1_1/${cloudName}/${type}/destroy`,
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Content-Length": Buffer.byteLength(body),
        },
      },
      (res) => {
        res.on("data", () => {});
        res.on("end", resolve);
      }
    );
    req.on("error", reject);
    req.write(body);
    req.end();
  });

  return { success: true };
});

