const express = require('express');
const auth = require('./controllers/authController');
const todo = require('./controllers/todoController');
const { requireAuth } = require('./middleware/auth');

const router = express.Router();

// Autentikasi
router.post('/auth/register', auth.register);
router.post('/auth/login', auth.login);
router.get('/auth/me', requireAuth, auth.me);

// Catatan & tugas (wajib login)
router.get('/todoozi', requireAuth, todo.list);
router.post('/todoozi', requireAuth, todo.create);
router.put('/todoozi/:id', requireAuth, todo.update);
router.delete('/todoozi/:id', requireAuth, todo.remove);

module.exports = router;
