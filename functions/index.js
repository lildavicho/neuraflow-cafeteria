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
  storageBucket: "baru-fe8a3.firebasestorage.app"
});

setGlobalOptions({ maxInstances: 10 });

const bucket = admin.storage().bucket();

// ----------------------------------------------------------
// 🚀 FUNCIÓN HTTP PARA GENERAR Y SUBIR UN QR
// ----------------------------------------------------------
exports.generateQR = onRequest({ region: "us-central1" }, async (req, res) => {
  try {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Método no permitido. Usa POST." });
      return;
    }

    const { text } = req.body || {};
    if (!text || String(text).trim() === "") {
      res.status(400).json({ error: "Se requiere un texto para generar el QR." });
      return;
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








