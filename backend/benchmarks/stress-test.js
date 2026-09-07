const http = require('http');

const TARGET_URL = 'http://localhost:3000/api/v1/restaurants';
const CONCURRENCY = 50; // Simultaneous connections
const TOTAL_REQUESTS = 500; // Total requests in batch

let completed = 0;
let successful = 0;
let failed = 0;
const latencies = [];

console.log(`\n============================================================`);
console.log(`🔥 VODIL EATS HIGH-CONCURRENCY BENCHMARK & LOAD TEST`);
console.log(`🎯 Target: ${TARGET_URL}`);
console.log(`⚡ Concurrency: ${CONCURRENCY} parallel streams`);
console.log(`📊 Total Requests: ${TOTAL_REQUESTS}`);
console.log(`============================================================\n`);

const startTime = Date.now();

function sendRequest() {
  const reqStart = Date.now();
  http.get(TARGET_URL, (res) => {
    let data = '';
    res.on('data', chunk => { data += chunk; });
    res.on('end', () => {
      const latency = Date.now() - reqStart;
      latencies.push(latency);
      if (res.statusCode >= 200 && res.statusCode < 400) {
        successful++;
      } else {
        failed++;
      }
      completed++;
      if (completed < TOTAL_REQUESTS) {
        sendRequest();
      } else if (completed === TOTAL_REQUESTS) {
        reportResults();
      }
    });
  }).on('error', (err) => {
    failed++;
    completed++;
    if (completed < TOTAL_REQUESTS) {
      sendRequest();
    } else if (completed === TOTAL_REQUESTS) {
      reportResults();
    }
  });
}

function reportResults() {
  const totalDurationSeconds = (Date.now() - startTime) / 1000;
  const avgLatency = latencies.reduce((a, b) => a + b, 0) / latencies.length;
  latencies.sort((a, b) => a - b);
  const p50 = latencies[Math.floor(latencies.length * 0.5)];
  const p95 = latencies[Math.floor(latencies.length * 0.95)];
  const p99 = latencies[Math.floor(latencies.length * 0.99)];
  const rps = Math.round(TOTAL_REQUESTS / totalDurationSeconds);

  console.log(`\n----------------- BENCHMARK RESULTS -----------------`);
  console.log(`✅ Completed Requests:   ${completed}/${TOTAL_REQUESTS}`);
  console.log(`🟢 Successful:           ${successful} (${Math.round((successful / completed) * 100)}%)`);
  console.log(`🔴 Failed / Errors:      ${failed}`);
  console.log(`⏱ Total Duration:       ${totalDurationSeconds.toFixed(2)} seconds`);
  console.log(`🚀 Throughput (RPS):     ${rps} req/sec`);
  console.log(`📊 Latency:`);
  console.log(`   - Average:            ${avgLatency.toFixed(1)} ms`);
  console.log(`   - Median (p50):       ${p50} ms`);
  console.log(`   - 95th Percentile:    ${p95} ms`);
  console.log(`   - 99th Percentile:    ${p99} ms`);
  console.log(`=====================================================\n`);
}

// Spawn workers
for (let i = 0; i < CONCURRENCY; i++) {
  sendRequest();
}
