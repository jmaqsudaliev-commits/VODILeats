import cluster from 'node:cluster';
import os from 'node:os';

const numCPUs = os.cpus().length;

if (cluster.isPrimary) {
  console.log(`\n============================================================`);
  console.log(`🚀 VODIL EATS HIGH-AVAILABILITY CLUSTER MASTER STARTED [PID: ${process.pid}]`);
  console.log(`⚡ Detected ${numCPUs} CPU Cores. Forking worker pool for 1,000,000+ users...`);
  console.log(`============================================================\n`);

  // Fork workers
  for (let i = 0; i < numCPUs; i++) {
    cluster.fork();
  }

  // Auto-restart if a worker crashes (zero-downtime resilience)
  cluster.on('exit', (worker, code, signal) => {
    console.warn(`⚠️ Worker ${worker.process.pid} died (code: ${code}, signal: ${signal}). Auto-restarting new worker...`);
    cluster.fork();
  });
} else {
  // Workers share the TCP connection on port 3000
  import('./main');
  console.log(`⚡ Cluster Worker ${process.pid} initialized and serving traffic.`);
}
