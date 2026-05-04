import 'dotenv/config';
import { createPool } from 'mysql2/promise';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';

function requireEnv(name) {
  const value = process.env[name];
  if (!value || value.trim() === '') {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

const pool = createPool({
  host: process.env.DB_HOST || '127.0.0.1',
  port: Number.parseInt(process.env.DB_PORT || '3310', 10),
  user: requireEnv('DB_USER'),
  password: requireEnv('DB_PASS'),
  database: requireEnv('DB_NAME'),
  connectionLimit: Number.parseInt(process.env.DB_POOL_LIMIT || '5', 10)
});

const serviceAccountPath = requireEnv('GOOGLE_APPLICATION_CREDENTIALS');
const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));
if (!getApps().length) {
  initializeApp({ credential: cert(serviceAccount) });
}
const firestore = getFirestore();
const pollIntervalMs = Number.parseInt(process.env.SYNC_POLL_INTERVAL_MS || '2000', 10);

console.log('Realtime sync started');

async function sync() {
  let cycle = 0;

  while (true) {
    cycle += 1;
    try {
      const [rows] = await pool.execute(
        "SELECT id, entity_type, entity_id, op_type, payload FROM sync_outbox WHERE status = 'PENDING' LIMIT 10"
      );

      for (const row of rows) {
        try {
          let data = {};
          if (row.payload) {
            try {
              data = JSON.parse(Buffer.isBuffer(row.payload) ? row.payload.toString('utf8') : row.payload);
            } catch {
              data = { _raw_data: 'Could not parse payload' };
            }
          }

          const docRef = firestore.collection(row.entity_type).doc(row.entity_id.toString());
          if (row.op_type === 'DELETE') {
            await docRef.delete();
          } else {
            await docRef.set(data, { merge: true });
          }

          await pool.execute(
            "UPDATE sync_outbox SET status = 'DONE', processed_at = NOW() WHERE id = ?",
            [row.id]
          );
          console.log(`[${cycle}] synced ${row.entity_type}.${row.entity_id}`);
        } catch (error) {
          console.error(`[${cycle}] row ${row.id} failed:`, error.message);
          await pool.execute("UPDATE sync_outbox SET status = 'ERROR' WHERE id = ?", [row.id]);
        }
      }

      if (rows.length === 0 && (cycle === 1 || cycle % 5 === 0)) {
        const [pending] = await pool.execute("SELECT COUNT(*) as count FROM sync_outbox WHERE status = 'PENDING'");
        const [total] = await pool.execute("SELECT COUNT(*) as count FROM sync_outbox");
        console.log(`[${cycle}] waiting for changes; pending ${pending[0].count}/${total[0].count}`);
      }

      await new Promise(resolve => setTimeout(resolve, pollIntervalMs));
    } catch (error) {
      console.error('sync loop failed:', error.message);
      await new Promise(resolve => setTimeout(resolve, Math.max(pollIntervalMs, 5000)));
    }
  }
}

process.on('SIGINT', async () => {
  console.log('Realtime sync stopped');
  await pool.end();
  process.exit(0);
});

sync();
