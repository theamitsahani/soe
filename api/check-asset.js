const admin = require("firebase-admin");
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

    const { visitId, schoolId, category, mediaId, publicId: reqPublicId, resourceType } = req.body || {};
    if (!visitId || !category) {
      return res.status(400).json({ error: "visitId and category are required." });
    }

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

    const result = await new Promise((resolve) => {
      const apiReq = https.request(options, (apiRes) => {
        let data = "";
        apiRes.on("data", (chunk) => { data += chunk; });
        apiRes.on("end", () => {
          if (apiRes.statusCode === 200) {
            try {
              const json = JSON.parse(data);
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
      apiReq.on("error", () => resolve({ exists: false }));
      apiReq.end();
    });

    return res.status(200).json(result);
  } catch (err) {
    console.error("Error in check-asset:", err);
    return res.status(500).json({ error: err.message || "Internal server error" });
  }
};
