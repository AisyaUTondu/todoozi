const bcrypt = require('bcryptjs');
const db = require('../db');
const { signToken } = require('../middleware/auth');

function avatarFor(username) {
  const template = process.env.AVATAR_URL_TEMPLATE;
  if (template === undefined) {
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(username)}&background=6C4FD1&color=ffffff&size=256&format=png`;
  }
  if (!template) return '';
  return template.replace('{username}', encodeURIComponent(username));
}

function publicUser(row) {
  return {
    id: Number(row.id),
    username: row.username,
    avatar_url: avatarFor(row.username),
  };
}

exports.register = async (req, res, next) => {
  try {
    const username = String((req.body && req.body.username) || '').trim();
    const password = String((req.body && req.body.password) || '');

    if (!/^[A-Za-z0-9_.]{3,30}$/.test(username)) {
      return res.status(422).json({
        message: 'Username 3-30 karakter (huruf, angka, titik, atau garis bawah)',
      });
    }
    if (password.length < 6) {
      return res.status(422).json({ message: 'Password minimal 6 karakter' });
    }

    const hash = bcrypt.hashSync(password, 10);
    const result = await db.query(
      `INSERT INTO users (username, password_hash)
       VALUES ($1, $2)
       RETURNING id, username`,
      [username, hash]
    );

    const user = { id: Number(result.rows[0].id), username: result.rows[0].username };
    return res.status(201).json({
      message: 'Registrasi berhasil',
      token: signToken(user),
      user: publicUser(user),
    });
  } catch (err) {
    if (err.code === '23505') {
      return res.status(409).json({ message: 'Username sudah terdaftar' });
    }
    return next(err);
  }
};

exports.login = async (req, res, next) => {
  try {
    const username = String((req.body && req.body.username) || '').trim();
    const password = String((req.body && req.body.password) || '');

    const result = await db.query(
      'SELECT id, username, password_hash FROM users WHERE lower(username) = lower($1) LIMIT 1',
      [username]
    );
    const row = result.rows[0];

    if (!row || !bcrypt.compareSync(password, row.password_hash)) {
      return res.status(401).json({ message: 'Username atau password salah' });
    }

    const user = { id: Number(row.id), username: row.username };
    return res.json({
      message: 'Login berhasil',
      token: signToken(user),
      user: publicUser(user),
    });
  } catch (err) {
    return next(err);
  }
};

exports.me = async (req, res, next) => {
  try {
    const result = await db.query(
      'SELECT id, username FROM users WHERE id = $1 LIMIT 1',
      [req.user.id]
    );
    const row = result.rows[0];
    if (!row) return res.status(401).json({ message: 'Akun tidak ditemukan' });
    return res.json({ user: publicUser(row) });
  } catch (err) {
    return next(err);
  }
};
