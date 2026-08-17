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
    await admin.auth().verifyIdToken(idToken);

    const { visitId, publicId, resourceType } = req.body || {};
    if (!visitId || !publicId) {
      return res.status(400).json({ error: "visitId and publicId are required." });
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
