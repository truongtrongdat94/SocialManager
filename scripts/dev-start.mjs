import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const backendDir = resolve(workspaceRoot, 'social-manager-backend');
const frontendDir = resolve(workspaceRoot, 'frontend');

const processes = [];
let shuttingDown = false;

function prefixStream(stream, prefix, target) {
  let buffer = '';

  stream.on('data', (chunk) => {
    buffer += chunk.toString();
    const lines = buffer.split(/\r?\n/);
    buffer = lines.pop() ?? '';

    for (const line of lines) {
      if (line.trim().length > 0) {
        target.write(`${prefix} ${line}\n`);
      }
    }
  });

  stream.on('end', () => {
    if (buffer.trim().length > 0) {
      target.write(`${prefix} ${buffer}\n`);
    }
    buffer = '';
  });
}

function runStep(name, command, args, cwd, env = {}) {
  const child = spawn(command, args, {
    cwd,
    env: { ...process.env, ...env },
    windowsHide: false,
    shell: false,
  });

  processes.push(child);
  prefixStream(child.stdout, `[${name}]`, process.stdout);
  prefixStream(child.stderr, `[${name}]`, process.stderr);

  child.on('exit', (code, signal) => {
    if (shuttingDown) {
      return;
    }

    if (code !== 0) {
      console.error(`[${name}] exited with code ${code ?? 'unknown'}${signal ? ` (signal ${signal})` : ''}`);
      shutdown(1);
    } else {
      console.log(`[${name}] completed`);
    }
  });

  return child;
}

function killProcessTree(pid) {
  if (!pid) {
    return;
  }

  spawn('taskkill', ['/PID', String(pid), '/T', '/F'], {
    cwd: workspaceRoot,
    env: process.env,
    windowsHide: true,
    shell: false,
    stdio: 'ignore',
  }).unref();
}

function shutdown(exitCode = 0) {
  if (shuttingDown) {
    return;
  }

  shuttingDown = true;

  for (const child of processes) {
    if (child.pid) {
      killProcessTree(child.pid);
    }
  }

  setTimeout(() => process.exit(exitCode), 250);
}

process.on('SIGINT', () => shutdown(0));
process.on('SIGTERM', () => shutdown(0));
process.on('uncaughtException', (error) => {
  console.error(error);
  shutdown(1);
});
process.on('unhandledRejection', (error) => {
  console.error(error);
  shutdown(1);
});

console.log('Starting SocialManager dev environment...');
console.log(`Workspace: ${workspaceRoot}`);
console.log('1) Bringing up Docker services...');

const compose = runStep('docker', 'cmd.exe', ['/c', 'docker compose up -d'], workspaceRoot);

compose.on('close', (code) => {
  if (code !== 0) {
    console.error('[docker] docker compose up -d failed. Fix Docker Desktop / compose first.');
    shutdown(1);
    return;
  }

  console.log('2) Starting backend on port 8080...');
  runStep('backend', 'cmd.exe', ['/c', '.\\mvnw.cmd spring-boot:run'], backendDir, {
    SPRING_PROFILES_ACTIVE: process.env.SPRING_PROFILES_ACTIVE ?? 'dev',
  });

  console.log('3) Starting frontend on port 3000...');
  runStep('frontend', 'cmd.exe', ['/c', 'npm.cmd run dev'], frontendDir);

  console.log('Dev environment is starting. Backend: http://localhost:8080 | Frontend: http://localhost:3000');
  console.log('Press Ctrl+C to stop everything.');
});
