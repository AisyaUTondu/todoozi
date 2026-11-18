require('dotenv').config();

const express = require('express');
const cors = require('cors');
const routes = require('./src/routes');
const db = require('./src/db');

if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 32) {
  throw new Error('JWT_SECRET wajib diatur di Environment Variables (minimal 32 karakter).');
}

const app = express();
app.set('trust proxy', 1);
app.use(cors());
app.use(express.json({ limit: '1mb' }));

app.get('/', (req, res) => res.json({ name: 'Todoozi API', status: 'ok' }));
app.get('/health', async (req, res) => {
  try {
    await db.query('SELECT 1');
    res.json({ status: 'ok', database: 'connected' });
  } catch (error) {
    console.error('[Health check]', error.message);
    res.status(503).json({ status: 'error', database: 'disconnected' });
  }
});

app.use('/api', routes);

app.use((req, res) => res.status(404).json({ message: 'Endpoint tidak ditemukan' }));
app.use((err, req, res, next) => {
  if (err.type === 'entity.parse.failed') {
    return res.status(400).json({ message: 'Format JSON tidak valid' });
  }
  console.error('[API error]', err);
  res.status(500).json({ message: 'Terjadi kesalahan pada server' });
});

const port = Number(process.env.PORT) || 3000;
const server = app.listen(port, '0.0.0.0', () => {
  console.log(`Todoozi API berjalan pada port ${port}`);
});

async function shutdown() {
  server.close(async () => {
    await db.end();
    process.exit(0);
  });
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
