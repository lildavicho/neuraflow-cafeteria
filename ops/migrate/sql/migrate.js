require('dotenv').config()
const mysql = require('mysql2/promise')
const admin = require('firebase-admin');
const { getFirestore } = require('firebase-admin/firestore');

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: process.env.FIREBASE_PROJECT_ID,
});

const databaseId = process.env.FIRESTORE_DATABASE_ID || 'default';
const db = getFirestore(admin.app(), databaseId);

module.exports = { db };
 

async function main() {
  console.log('🚀 Conectando a MySQL...')
  const conn = await mysql.createConnection({
    host: process.env.MYSQL_HOST,
    port: process.env.MYSQL_PORT,
    user: process.env.MYSQL_USER,
    password: process.env.MYSQL_PASSWORD,
    multipleStatements: true
  })

  await conn.query(`USE \`${process.env.MYSQL_DB_MAIN}\``)
  console.log('📦 Base de datos principal seleccionada:', process.env.MYSQL_DB_MAIN)

  const [users] = await conn.query(`SELECT * FROM users`)
  for (const u of users) {
    await db.collection('users').doc(String(u.id)).set(u)
  }

  console.log('✅ Migración completa.')
  await conn.end()
}

main().catch(err => {
  console.error('❌ Error en la migración:', err)
  process.exit(1)
})
