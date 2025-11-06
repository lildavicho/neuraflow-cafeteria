// ----------------------------------------------------------
// 🔧 IMPORTACIÓN DE DEPENDENCIAS
// ----------------------------------------------------------
const { onRequest } = require("firebase-functions/v2/https");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const QRCode = require("qrcode");

// ----------------------------------------------------------
// ⚙️ CONFIGURACIÓN GLOBAL DE FIREBASE ADMIN
// ----------------------------------------------------------
admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  storageBucket: `${process.env.GCLOUD_PROJECT}.appspot.com`
});

setGlobalOptions({ maxInstances: 10 });

const bucket = admin.storage().bucket();
const rateMap = new Map();

// ----------------------------------------------------------
// 🚀 FUNCIÓN HTTP PARA GENERAR Y SUBIR UN QR
// ----------------------------------------------------------
exports.generateQR = onRequest({ region: "us-central1" }, async (req, res) => {
  try {
    if (req.method !== "POST") {
      return res.status(405).json({ error: "Método no permitido. Usa POST." });
    }

    const authHeader = req.get("authorization") || "";
    if (!authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "Token requerido" });
    }
    const token = authHeader.substring(7);
    const decoded = await admin.auth().verifyIdToken(token);
    const uid = decoded.uid;

    const now = Date.now();
    const entry = rateMap.get(uid) || { count: 0, resetAt: now };
    if (now - entry.resetAt > 60000) {
      entry.count = 0;
      entry.resetAt = now;
    }
    if (entry.count >= 10) {
      return res.status(429).json({ error: "Rate limit alcanzado. Intenta en un minuto." });
    }
    entry.count += 1;
    rateMap.set(uid, entry);

    const { text } = req.body || {};
    if (!text || String(text).trim() === "") {
      return res.status(400).json({ error: "Se requiere un texto para generar el QR." });
    }

    // Generar el QR
    const qrBuffer = await QRCode.toBuffer(text);

    // Crear nombre de archivo
    const timestamp = Date.now();
    const filePath = `Productos/QRs/qr_${timestamp}.png`;
    const file = bucket.file(filePath);

    // Guardar QR en el bucket
    await file.save(qrBuffer, { contentType: "image/png" });

    // Generar URL firmada (válida hasta 2057 aprox.)
    const [url] = await file.getSignedUrl({
      action: "read",
      expires: "2057-01-01"
    });

    res.status(200).json({
      message: "✅ QR generado correctamente",
      qrUrl: url,
      bucket: bucket.name,
      path: filePath
    });
  } catch (error) {
    console.error("❌ Error al generar QR:", error);
    res.status(500).json({ error: "Error interno del servidor." });
  }
});







