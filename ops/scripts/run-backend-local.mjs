import { spawn } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parse as parseDotenv } from 'dotenv';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..', '..');
const inheritedEnv = new Set(Object.entries(process.env)
  .filter(([, value]) => value !== undefined && value !== '')
  .map(([key]) => key));

const envFiles = [
  path.join(repoRoot, '.env'),
  path.join(repoRoot, 'apps', 'api', '.env'),
];

const loadedEnvFiles = [];

for (const envFile of envFiles) {
  if (existsSync(envFile)) {
    loadedEnvFiles.push(path.relative(repoRoot, envFile));
    const parsedEnv = parseDotenv(readFileSync(envFile, 'utf8'));

    for (const [key, value] of Object.entries(parsedEnv)) {
      if (inheritedEnv.has(key)) {
        continue;
      }

      if (value === '' && process.env[key]) {
        continue;
      }

      process.env[key] = value;
    }
  }
}

const requiredEnv = ['DB_URL', 'DB_USER', 'DB_PASS', 'JWT_SECRET'];
const missingEnv = requiredEnv.filter((key) => !process.env[key]?.trim());

if (missingEnv.length > 0) {
  console.error(`[ERROR] Missing required local backend environment variables: ${missingEnv.join(', ')}`);
  console.error(`[ERROR] Loaded env files: ${loadedEnvFiles.length > 0 ? loadedEnvFiles.join(', ') : 'none'}`);
  console.error('[ERROR] Set DB_PASS in .env or apps/api/.env to the real PostgreSQL/Supabase database password.');
  console.error('[ERROR] Postgres is requesting SCRAM authentication, so the backend cannot start with an empty DB_PASS.');
  process.exit(1);
}

const firebaseEnabled = `${process.env.FIREBASE_ENABLED ?? ''}`.toLowerCase() === 'true';
const firebasePath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH?.trim();
if (firebaseEnabled && firebasePath && !firebasePath.startsWith('classpath:') && !existsSync(firebasePath)) {
  console.warn(`[WARN] FIREBASE_ENABLED=true pero no existe FIREBASE_SERVICE_ACCOUNT_PATH: ${firebasePath}`);
  console.warn('[WARN] Firebase Admin se desactiva solo para este arranque local. Configura el JSON real para habilitarlo.');
  process.env.FIREBASE_ENABLED = 'false';
}

const backendPort = Number.parseInt(process.env.SERVER_PORT || '8081', 10);

if (Number.isNaN(backendPort) || backendPort < 1 || backendPort > 65535) {
  console.error(`[ERROR] SERVER_PORT invalido: ${process.env.SERVER_PORT}`);
  process.exit(1);
}

await assertPortAvailable(backendPort);

const command = process.platform === 'win32' ? 'cmd.exe' : 'mvn';
const mavenArgs = [
  '-f',
  'apps/api/pom.xml',
  'clean',
  'spring-boot:run',
  '-Dspring-boot.run.profiles=local',
];
const args = process.platform === 'win32'
  ? ['/c', 'mvn', ...mavenArgs]
  : mavenArgs;

const child = spawn(command, args, {
  cwd: repoRoot,
  stdio: 'inherit',
  env: process.env,
});

child.on('exit', (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code ?? 0);
});

function assertPortAvailable(port) {
  return new Promise((resolve) => {
    const server = net.createServer();

    server.once('error', (error) => {
      if (error.code === 'EADDRINUSE') {
        console.error(`[ERROR] El backend ya esta corriendo o el puerto ${port} esta ocupado.`);
        console.error(`[ERROR] Cierra el proceso que usa ${port} antes de ejecutar npm run backend:start:local otra vez.`);
        process.exit(1);
      }

      console.warn(`[WARN] No se pudo validar el puerto ${port}: ${error.message}`);
      console.warn('[WARN] Se continua el arranque; Spring Boot reportara el error real si el puerto no esta disponible.');
      resolve();
    });

    server.once('listening', () => {
      server.close(() => resolve());
    });

    server.listen(port, '127.0.0.1');
  });
}
