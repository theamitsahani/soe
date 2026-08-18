const admin = require("firebase-admin");
const crypto = require("crypto");
const https = require("https");

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
 * This matters most here: without it, any authenticated employee could permanently
 * destroy any other employee's Cloudinary photos/videos by guessing/observing a publicId.
 * Also blocks deletion once a visit is REVIEWED, matching the Firestore photos-subcollection
 * lock rule.
 */
async function authorizeVisitAccess(uid, visitId, requireNotReviewed) {
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
    const visitData = visitSnap.data();
    if (visitData.employeeId !== uid) {
      return { allowed: false, reason: "Not authorized for this visit." };
    }
    if (requireNotReviewed && visitData.status === "REVIEWED") {
      return { allowed: false, reason: "Visit is locked and can no longer be modified." };
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

    const { visitId, publicId, resourceType } = req.body || {};
    if (!visitId || !publicId) {
      return res.status(400).json({ error: "visitId and publicId are required." });
    }

    const authResult = await authorizeVisitAccess(decoded.uid, visitId, true);
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
      signature
    }).toString();

    await new Promise((resolve, reject) => {
      const apiReq = https.request(
        {
          hostname: "api.cloudinary.com",
          path: `/v1_1/${cloudName}/${type}/destroy`,
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Content-Length": Buffer.byteLength(body)
          }
        },
        (apiRes) => {
          apiRes.on("data", () => {});
          apiRes.on("end", resolve);
        }
      );
      apiReq.on("error", reject);
      apiReq.write(body);
      apiReq.end();
    });

    return res.status(200).json({ success: true });
  } catch (err) {
    console.error("Error in delete-asset:", err);
    return res.status(500).json({ error: err.message || "Internal server error" });
  }
};
