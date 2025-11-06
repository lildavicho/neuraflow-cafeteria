// sync-ultimate.js - Versión definitiva
import { createPool } from 'mysql2/promise';
import { initializeApp, cert, getApps } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import fs from 'fs';

console.log('🚀 SINCRONIZACIÓN ULTIMATE - UCACUE ERP');
console.log('========================================');

// Configuración directa (sin .env para evitar problemas)
const pool = createPool({
  host: '127.0.0.1',
  port: 3310,
  user: 'ucacue_user', 
  password: 'ucacue_pass',
  database: 'ucacue_erp'
});

// Firebase
const serviceAccount = JSON.parse(fs.readFileSync('./serviceAccount.json', 'utf8'));
if (!getApps().length) initializeApp({ credential: cert(serviceAccount) });
const firestore = getFirestore();

console.log('✅ Todas las conexiones establecidas\n');

async function sync() {
  let cycle = 0;
  
  while (true) {
    cycle++;
    try {
      // Consulta SIMPLE Y DIRECTA
      const [rows] = await pool.execute(
        "SELECT id, entity_type, entity_id, op_type, payload FROM sync_outbox WHERE status = 'PENDING' LIMIT 10"
      );
      
      if (rows.length > 0) {
        console.log(`📦 CICLO ${cycle}: ${rows.length} REGISTROS PENDIENTES`);
        console.log('─────────────────────────────────────');
        
        for (const row of rows) {
          console.log(`   🔄 [${row.id}] ${row.entity_type}.${row.entity_id} (${row.op_type})`);
          
          try {
            // Procesar payload de manera SEGURA
            let data = {};
            if (row.payload) {
              try {
                if (Buffer.isBuffer(row.payload)) {
                  data = JSON.parse(row.payload.toString('utf8'));
                } else {
                  data = JSON.parse(row.payload);
                }
              } catch (e) {
                data = { _raw_data: 'Could not parse payload' };
              }
            }
            
            // Firestore
            const docRef = firestore.collection(row.entity_type).doc(row.entity_id.toString());
            
            if (row.op_type === 'DELETE') {
              await docRef.delete();
              console.log(`   ✅ ELIMINADO`);
            } else {
              await docRef.set(data, { merge: true });
              console.log(`   ✅ SINCRONIZADO`);
            }
            
            // Marcar como completado
            await pool.execute(
              "UPDATE sync_outbox SET status = 'DONE', processed_at = NOW() WHERE id = ?",
              [row.id]
            );
            
          } catch (error) {
            console.log(`   ❌ ERROR: ${error.message}`);
            await pool.execute(
              "UPDATE sync_outbox SET status = 'ERROR' WHERE id = ?",
              [row.id]
            );
          }
        }
        console.log('─────────────────────────────────────\n');
      } else {
        if (cycle === 1 || cycle % 5 === 0) {
          console.log(`⏳ CICLO ${cycle}: Esperando cambios...`);
          
          // Estadísticas rápidas
          const [pending] = await pool.execute("SELECT COUNT(*) as count FROM sync_outbox WHERE status = 'PENDING'");
          const [total] = await pool.execute("SELECT COUNT(*) as count FROM sync_outbox");
          
          console.log(`   📊 Sync Outbox: ${pending[0].count}/${total[0].count} pendientes`);
          
          if (pending[0].count === 0 && cycle > 5) {
            console.log('   💡 PRUEBA: Ejecuta en MySQL: UPDATE products SET price = 2.50 WHERE id = 101;');
          }
        }
      }
      
      // Esperar 2 segundos
      await new Promise(resolve => setTimeout(resolve, 2000));
      
    } catch (error) {
      console.log('❌ Error general:', error.message);
      await new Promise(resolve => setTimeout(resolve, 5000));
    }
  }
}

// Manejo de cierre
process.on('SIGINT', async () => {
  console.log('\n🛑 Sincronización detenida');
  await pool.end();
  process.exit(0);
});

sync();

