const jwt = require('jsonwebtoken');

function getSecret() {
  const secret = process.env.JWT_SECRET;
  if (!secret || secret.length < 32) {
    throw new Error('JWT_SECRET belum diatur dengan benar.');
  }
  return secret;
}

function signToken(user) {
  return jwt.sign(
    { sub: Number(user.id), username: user.username },
    getSecret(),
    { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
  );
}

function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const [scheme, token] = header.split(' ');
  if (scheme !== 'Bearer' || !token) {
    return res.status(401).json({ message: 'Token tidak ditemukan' });
  }
  try {
    const payload = jwt.verify(token, getSecret());
    req.user = { id: Number(payload.sub), username: payload.username };
    if (!Number.isSafeInteger(req.user.id) || req.user.id <= 0) {
      return res.status(401).json({ message: 'Token tidak valid' });
    }
    return next();
  } catch (err) {
    return res.status(401).json({ message: 'Token tidak valid atau sudah kedaluwarsa' });
  }
}

module.exports = { signToken, requireAuth };
