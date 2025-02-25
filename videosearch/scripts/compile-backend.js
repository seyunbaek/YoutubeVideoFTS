import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

// Get __dirname equivalent in ES modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

function compileBackend() {
  // Navigate up two levels to reach the root directory with pom.xml
  const PROJECT_ROOT = join(__dirname, '../..');

  // Run maven compile and copy dependencies
  const mvnProcess = spawn('mvn', [
    'compile',
    'dependency:copy-dependencies'
  ], {
    stdio: 'inherit',
    cwd: PROJECT_ROOT
  });

  mvnProcess.on('error', (err) => {
    console.error('Failed to compile Java backend:', err);
    if (err.code === 'ENOENT') {
      console.error('Error: Maven not found. Make sure Maven is installed and in your PATH.');
    }
    process.exit(1);
  });

  mvnProcess.on('close', (code) => {
    if (code !== 0) {
      console.error(`Maven process exited with code ${code}`);
      process.exit(code);
    }
  });
}

compileBackend();