const admin = require("firebase-admin");
const crypto = require("crypto");

if (!admin.apps.length) {
  try {
    admin.initializeApp();
  } catch (e) {
    console.error("Firebase admin init error:", e);
  }
}

/**
 * Server-side authorization: caller must be an active Admin, or the employee the
 * given visit is actually assigned to. Mirrors the ownership checks in firestore.rules,
 * because these Vercel endpoints use the Admin SDK and bypass Firestore rules entirely.
 */
async function authorizeVisitAccess(uid, visitId) {
  try {
    const db = admin.firestore();
    const userSnap = await db.collection("users").doc(uid).get();
    const userData = userSnap.exists ? userSnap.data() : null;
    const isActiveAdmin =
      !!userData &&
      userData.role === "ADMIN" &&
      userData.status !== "INACTIVE" &&
      userData.isDeleted !== true;

    if (isActiveAdmin) {
      return { allowed: true };
    }

    if (!userData || userData.status === "INACTIVE" || userData.isDeleted === true) {
      return { allowed: false, reason: "Account is not active." };
    }

    const visitSnap = await db.collection("visits").doc(visitId).get();
    if (!visitSnap.exists) {
      return { allowed: false, reason: "Visit not found." };
    }
    if (visitSnap.data().employeeId !== uid) {
      return { allowed: false, reason: "Not authorized for this visit." };
    }
    return { allowed: true };
  } catch (e) {
    console.error("authorizeVisitAccess error:", e);
    return { allowed: false, reason: "Authorization check failed." };
  }
}

module.exports = async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  try {
    const authHeader = req.headers.authorization || req.headers.Authorization || "";
    if (!authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "Unauthenticated" });
    }
    const idToken = authHeader.split("Bearer ")[1];
    const decoded = await admin.auth().verifyIdToken(idToken);

    const { visitId, schoolId, category, mediaId, publicId: reqPublicId, mediaIndex } = req.body || {};
    if (!visitId || !schoolId || !category) {
      return res.status(400).json({ error: "visitId, schoolId, category are required." });
    }

    // BUG FIX (security): previously this endpoint only checked that the caller had SOME
    // valid Firebase login, not that they owned this specific visit. Any authenticated
    // employee could request a signature for ANY visitId and upload/overwrite photos into
    // another employee's report. Enforce the same ownership rule the Firestore rules use:
    // admin, or the visit's own assigned employee.
    const authResult = await authorizeVisitAccess(decoded.uid, visitId);
    if (!authResult.allowed) {
      return res.status(403).json({ error: authResult.reason });
    }

    const cloudName = process.env.CLOUDINARY_CLOUD_NAME || "";
    const apiKey = process.env.CLOUDINARY_API_KEY || "";
    const apiSecret = process.env.CLOUDINARY_API_SECRET || "";

    if (!cloudName || !apiKey || !apiSecret) {
      return res.status(500).json({ error: "Cloudinary credentials not configured." });
    }

    const timestamp = Math.floor(Date.now() / 1000);
    const folder = `visits/${visitId}/${schoolId}/${category}`;
    const cleanIndex = (typeof mediaIndex === 'number' && mediaIndex >= 0) ? mediaIndex : 0;
    const publicId = mediaId || reqPublicId || `${visitId}_${category}_${cleanIndex}`;

    const paramsToSign = { folder, public_id: publicId, timestamp };
    const sortedString = Object.keys(paramsToSign)
      .sort()
      .map((k) => `${k}=${paramsToSign[k]}`)
      .join("&");
    const signature = crypto
      .createHash("sha1")
      .update(sortedString + apiSecret)
      .digest("hex");

    return res.status(200).json({
      cloudName,
      apiKey,
      timestamp: String(timestamp),
      folder,
      publicId,
      signature
    });
  } catch (err) {
    console.error("Error in get-signature:", err);
    return res.status(500).json({ error: err.message || "Internal server error" });
  }
};
