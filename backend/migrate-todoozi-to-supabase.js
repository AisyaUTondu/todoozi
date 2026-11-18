/**
 * migrate-todoozi-to-supabase.js
 *
 * Memindahkan data SQLite Todoozi (tabel users & todoozi) ke Supabase PostgreSQL.
 * - SQLite dibuka READ-ONLY (database lama tidak diubah).
 * - Semua insert dalam SATU transaksi; jika gagal, di-ROLLBACK.
 * - Menolak berjalan jika tabel tujuan sudah berisi data.
 * - Kolom dicocokkan otomatis berdasarkan nama (ID, password hash, relasi ikut terbawa).
 *
 * Pemakaian (dari folder backend):
 *   npm install pg
 *   node migrate-todoozi-to-supabase.js
 *
 * Opsional: SQLITE_PATH=./data/todoozi.db di .env untuk mengubah lokasi database.
 */

const fs = require('fs');
const path = require('path');

// ---------- Muat .env ----------
const envPath = path.join(__dirname, '.env');
try {
  require('dotenv').config({ path: envPath });
} catch (_) {
  if (fs.existsSync(envPath)) {
    for (const line of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
      const m = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/);
      if (m && process.env[m[1]] === undefined) {
        process.env[m[1]] = m[2].replace(/^['"]|['"]$/g, '');
      }
    }
  }
}

const { Client } = require('pg');

const DATABASE_URL = process.env.DATABASE_URL;
const SQLITE_PATH = path.resolve(
  __dirname,
  process.env.SQLITE_PATH || path.join('data', 'todoozi.db')
);

// Urutan penting: users dulu, baru todoozi (relasi).
const TABLES = ['users', 'todoozi'];

// ---------- Buka SQLite (read-only) ----------
function openSqlite(file) {
  if (!fs.existsSync(file)) {
    throw new Error(`File SQLite tidak ditemukan: ${file}`);
  }
  try {
    const Database = require('better-sqlite3');
    const db = new Database(file, { readonly: true });
    return { all: (sql) => db.prepare(sql).all(), close: () => db.close() };
  } catch (e) {
    if (e.code !== 'MODULE_NOT_FOUND') throw e;
  }
  try {
    const { DatabaseSync } = require('node:sqlite');
    const db = new DatabaseSync(file, { readOnly: true });
    return { all: (sql) => db.prepare(sql).all(), close: () => db.close() };
  } catch (e) {
    throw new Error(
      'Library SQLite tidak ditemukan. Pastikan better-sqlite3 terpasang ' +
        '(biasanya sudah ada di backend), atau gunakan Node.js 22+.'
    );
  }
}

const q = (name) => `"${String(name).replace(/"/g, '""')}"`;

async function main() {
  if (!DATABASE_URL) {
    throw new Error('DATABASE_URL belum diisi di file .env');
  }

  const sqlite = openSqlite(SQLITE_PATH);
  const pg = new Client({
    connectionString: DATABASE_URL,
    ssl: { rejectUnauthorized: false },
  });
  await pg.connect();

  try {
    // Cocokkan nama tabel SQLite (tidak peka huruf besar/kecil)
    const sqliteTables = sqlite
      .all("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
      .map((r) => r.name);

    const source = {};
    for (const t of TABLES) {
      const found = sqliteTables.find((n) => n.toLowerCase() === t);
      if (!found) {
        throw new Error(
          `Tabel "${t}" tidak ada di SQLite. Tabel yang ada: ${sqliteTables.join(', ') || '(kosong)'}. ` +
            'Sesuaikan array TABLES di script ini.'
        );
      }
      source[t] = found;
    }

    // Baca data lokal
    const data = {};
    for (const t of TABLES) {
      data[t] = sqlite.all(`SELECT * FROM ${q(source[t])}`);
    }
    console.log(
      'Data lokal ditemukan: ' + TABLES.map((t) => `${data[t].length} ${t}`).join(', ')
    );

    // Tolak jika tujuan sudah berisi data
    for (const t of TABLES) {
      const { rows } = await pg.query(`SELECT COUNT(*)::int AS n FROM public.${q(t)}`);
      if (rows[0].n > 0) {
        throw new Error(
          `Tabel Supabase "${t}" sudah berisi ${rows[0].n} baris. Impor dibatalkan agar data tidak dobel.`
        );
      }
    }

    await pg.query('BEGIN');

    for (const t of TABLES) {
      const rows = data[t];
      if (rows.length === 0) continue;

      const colInfo = await pg.query(
        `SELECT column_name, data_type FROM information_schema.columns
         WHERE table_schema = 'public' AND table_name = $1`,
        [t]
      );
      const pgCols = new Map(colInfo.rows.map((c) => [c.column_name, c.data_type]));

      const srcCols = Object.keys(rows[0]);
      const cols = srcCols.filter((c) => pgCols.has(c));
      const skipped = srcCols.filter((c) => !pgCols.has(c));
      if (skipped.length) {
        console.warn(`Peringatan [${t}]: kolom SQLite tidak ada di Supabase dan dilewati: ${skipped.join(', ')}`);
      }
      if (cols.length === 0) {
        throw new Error(`Tidak ada kolom yang cocok untuk tabel "${t}".`);
      }

      const sql =
        `INSERT INTO public.${q(t)} (${cols.map(q).join(', ')}) ` +
        `OVERRIDING SYSTEM VALUE VALUES (${cols.map((_, i) => `$${i + 1}`).join(', ')})`;

      for (const row of rows) {
        const values = cols.map((c) => {
          const v = row[c];
          if (pgCols.get(c) === 'boolean' && (v === 0 || v === 1)) return v === 1;
          return v === undefined ? null : v;
        });
        await pg.query(sql, values);
      }

      // Sinkronkan sequence/auto-increment agar insert berikutnya tidak bentrok
      for (const c of cols) {
        const seq = await pg.query(`SELECT pg_get_serial_sequence($1, $2) AS s`, [
          `public.${q(t)}`,
          c,
        ]);
        const s = seq.rows[0].s;
        if (s) {
          await pg.query(
            `SELECT setval($1, COALESCE((SELECT MAX(${q(c)}) FROM public.${q(t)}), 1),
                           (SELECT MAX(${q(c)}) IS NOT NULL FROM public.${q(t)}))`,
            [s]
          );
        }
      }
    }

    // Verifikasi jumlah sebelum commit
    const counts = {};
    for (const t of TABLES) {
      const { rows } = await pg.query(`SELECT COUNT(*)::int AS n FROM public.${q(t)}`);
      counts[t] = rows[0].n;
      if (counts[t] !== data[t].length) {
        throw new Error(
          `Jumlah tidak sama untuk "${t}": SQLite ${data[t].length}, Supabase ${counts[t]}`
        );
      }
    }

    await pg.query('COMMIT');
    console.log('BERHASIL: data SQLite sudah dipindahkan ke Supabase.');
    console.log('Verifikasi: ' + TABLES.map((t) => `${counts[t]} ${t}`).join(', '));
  } catch (err) {
    try {
      await pg.query('ROLLBACK');
    } catch (_) {}
    throw err;
  } finally {
    await pg.end();
    sqlite.close();
  }
}

main().catch((err) => {
  console.error('GAGAL:', err.message);
  console.error('Semua perubahan di Supabase dibatalkan (rollback). Database SQLite tidak diubah.');
  process.exit(1);
});
