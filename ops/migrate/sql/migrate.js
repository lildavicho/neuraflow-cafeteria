import 'dotenv/config';
import { createConnection } from 'mysql2/promise';
import { applicationDefault, getApp, getApps, initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';

function firstEnv(names, required = true) {
  for (const name of names) {
    const value = process.env[name];
    if (value && value.trim() !== '') {
      return value;
    }
  }
  if (required) {
    throw new Error(`Missing required environment variable: ${names.join(' or ')}`);
  }
  return undefined;
}

if (!getApps().length) {
  initializeApp({
    credential: applicationDefault(),
    projectId: firstEnv(['FIREBASE_PROJECT_ID'], false)
  });
}

const databaseId = process.env.FIRESTORE_DATABASE_ID || 'default';
const db = getFirestore(getApp(), databaseId);

async function main() {
  const databaseName = firstEnv(['DB_NAME', 'MYSQL_DB_MAIN', 'MYSQL_DATABASE']);
  const conn = await createConnection({
    host: firstEnv(['DB_HOST', 'MYSQL_HOST'], false) || '127.0.0.1',
    port: Number.parseInt(firstEnv(['DB_PORT', 'MYSQL_PORT'], false) || '3310', 10),
    user: firstEnv(['DB_USER', 'MYSQL_USER']),
    password: firstEnv(['DB_PASS', 'MYSQL_PASSWORD']),
    multipleStatements: true
  });

  await conn.query(`USE \`${databaseName}\``);
  console.log('MySQL source database selected:', databaseName);

  const [users] = await conn.query('SELECT * FROM users');
  for (const user of users) {
    await db.collection('users').doc(String(user.id)).set(user);
  }

  console.log('Migration completed');
  await conn.end();
}

main().catch(error => {
  console.error('Migration failed:', error);
  process.exit(1);
});
