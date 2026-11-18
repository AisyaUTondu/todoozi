const db = require('../db');

const CATEGORIES = ['NOTE', 'TASK'];

function toJson(row) {
  return {
    id: Number(row.id),
    title: row.title,
    content: row.content,
    category: row.category,
    is_completed: Boolean(row.is_completed),
    due_date: row.due_date ? String(row.due_date).slice(0, 10) : '',
  };
}

function parseBody(body, existing) {
  const b = body || {};
  const title = b.title !== undefined ? String(b.title).trim() : existing && existing.title;
  const content = b.content !== undefined && b.content !== null
    ? String(b.content) : (existing ? existing.content : '');
  const category = b.category !== undefined ? String(b.category).toUpperCase()
    : existing && existing.category;
  const due = b.due_date !== undefined && b.due_date !== null
    ? String(b.due_date).trim() : (existing ? String(existing.due_date || '').slice(0, 10) : '');
  const completed = b.is_completed !== undefined
    ? (b.is_completed === true || b.is_completed === 1 || b.is_completed === 'true')
    : (existing ? Boolean(existing.is_completed) : false);

  if (!title) return { error: 'Judul wajib diisi' };
  if (title.length > 200) return { error: 'Judul maksimal 200 karakter' };
  if (content.length > 5000) return { error: 'Isi maksimal 5000 karakter' };
  if (!CATEGORIES.includes(category)) return { error: 'Kategori harus NOTE atau TASK' };
  if (due !== '' && !/^\d{4}-\d{2}-\d{2}$/.test(due)) {
    return { error: 'Format due_date harus yyyy-MM-dd' };
  }

  return {
    value: {
      title,
      content,
      category,
      due_date: category === 'NOTE' ? '' : due,
      is_completed: completed,
    },
  };
}

exports.list = async (req, res, next) => {
  try {
    const { category } = req.query;
    let result;

    if (category) {
      const normalized = String(category).toUpperCase();
      if (!CATEGORIES.includes(normalized)) {
        return res.status(422).json({ message: 'Kategori harus NOTE atau TASK' });
      }
      result = await db.query(
        `SELECT id, title, content, category, is_completed, due_date
         FROM todoozi
         WHERE user_id = $1 AND category = $2
         ORDER BY id DESC`,
        [req.user.id, normalized]
      );
    } else {
      result = await db.query(
        `SELECT id, title, content, category, is_completed, due_date
         FROM todoozi WHERE user_id = $1 ORDER BY id DESC`,
        [req.user.id]
      );
    }
    return res.json({ data: result.rows.map(toJson) });
  } catch (err) {
    return next(err);
  }
};

exports.create = async (req, res, next) => {
  try {
    const { error, value } = parseBody(req.body, null);
    if (error) return res.status(422).json({ message: error });

    const result = await db.query(
      `INSERT INTO todoozi (user_id, title, content, category, is_completed, due_date)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id, title, content, category, is_completed, due_date`,
      [req.user.id, value.title, value.content, value.category, value.is_completed, value.due_date]
    );
    return res.status(201).json({ data: toJson(result.rows[0]) });
  } catch (err) {
    return next(err);
  }
};

exports.update = async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isSafeInteger(id) || id <= 0) {
      return res.status(404).json({ message: 'Data tidak ditemukan' });
    }

    const found = await db.query(
      `SELECT id, title, content, category, is_completed, due_date
       FROM todoozi WHERE id = $1 AND user_id = $2`,
      [id, req.user.id]
    );
    const existing = found.rows[0];
    if (!existing) return res.status(404).json({ message: 'Data tidak ditemukan' });

    const { error, value } = parseBody(req.body, existing);
    if (error) return res.status(422).json({ message: error });

    const result = await db.query(
      `UPDATE todoozi
       SET title = $1, content = $2, category = $3, is_completed = $4,
           due_date = $5, updated_at = CURRENT_TIMESTAMP
       WHERE id = $6 AND user_id = $7
       RETURNING id, title, content, category, is_completed, due_date`,
      [value.title, value.content, value.category, value.is_completed,
       value.due_date, id, req.user.id]
    );
    return res.json({ data: toJson(result.rows[0]) });
  } catch (err) {
    return next(err);
  }
};

exports.remove = async (req, res, next) => {
  try {
    const id = Number(req.params.id);
    if (!Number.isSafeInteger(id) || id <= 0) {
      return res.status(404).json({ message: 'Data tidak ditemukan' });
    }
    const result = await db.query(
      'DELETE FROM todoozi WHERE id = $1 AND user_id = $2',
      [id, req.user.id]
    );
    if (result.rowCount === 0) {
      return res.status(404).json({ message: 'Data tidak ditemukan' });
    }
    return res.json({ message: 'Data dihapus' });
  } catch (err) {
    return next(err);
  }
};
