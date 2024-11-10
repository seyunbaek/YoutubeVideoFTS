import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// Get __dirname equivalent in ES modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function runBackend() {
  const PROJECT_ROOT = join(__dirname, '../..');

  const javaProcess = spawn('java', [
    '-cp',
    `${PROJECT_ROOT}/target/classes:${PROJECT_ROOT}/target/dependency/*`,  // Use ; instead of : on Windows
    'operators.BackendEndpoint'
  ], {
    stdio: 'inherit',
    cwd: PROJECT_ROOT
  });

  javaProcess.on('error', (err) => {
    console.error('Failed to start Java backend:', err);
    if (err.code === 'ENOENT') {
      console.error('Error: Java not found. Make sure Java is installed and in your PATH.');
    }
    console.error('Project Root:', PROJECT_ROOT);
    console.error('Current working directory:', process.cwd());
  });

  javaProcess.on('close', (code) => {
    if (code !== 0) {
      console.log(`Java backend process exited with code ${code}`);
    }
  });

  // Handle shutdown
  process.on('SIGINT', () => {
    javaProcess.kill();
    process.exit();
  });

  process.on('SIGTERM', () => {
    javaProcess.kill();
    process.exit();
  });
}

runBackend();