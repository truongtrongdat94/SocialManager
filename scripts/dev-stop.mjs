#!/usr/bin/env node

import { spawn } from 'child_process';
import { platform } from 'os';

const isWindows = platform() === 'win32';

console.log('🛑 Stopping SocialManager dev stack...\n');

/**
 * Kill processes by name (Windows: taskkill, Unix: pkill)
 */
function killProcess(processName) {
  return new Promise((resolve) => {
    if (isWindows) {
      spawn('taskkill', ['/IM', processName, '/F'], { stdio: 'ignore' }).on('exit', resolve);
    } else {
      spawn('pkill', ['-f', processName], { stdio: 'ignore' }).on('exit', resolve);
    }
  });
}

async function stopStack() {
  try {
    // Stop frontend dev server (Vite)
    console.log('⏹️  Stopping frontend (Vite dev server)...');
    await killProcess(isWindows ? 'node.exe' : 'node');

    // Stop backend (Maven wrapper / Java)
    console.log('⏹️  Stopping backend (Spring Boot)...');
    await killProcess(isWindows ? 'java.exe' : 'java');

    // Give processes a moment to terminate
    await new Promise((r) => setTimeout(r, 1000));

    // Stop Docker containers
    console.log('⏹️  Stopping Docker containers...');
    return new Promise((resolve) => {
      const dockerStop = spawn(isWindows ? 'cmd' : 'bash', 
        isWindows ? ['/c', 'docker compose down'] : ['-c', 'docker compose down'],
        { cwd: process.cwd(), stdio: 'inherit' }
      );

      dockerStop.on('exit', (code) => {
        if (code === 0) {
          console.log('\n✅ SocialManager dev stack stopped successfully!');
          resolve(0);
        } else {
          console.log('\n⚠️  Docker stop completed with code:', code);
          resolve(code);
        }
      });

      dockerStop.on('error', (err) => {
        console.error('❌ Error stopping stack:', err);
        resolve(1);
      });
    });
  } catch (err) {
    console.error('❌ Error during shutdown:', err);
    process.exit(1);
  }
}

stopStack().then(code => process.exit(code));
