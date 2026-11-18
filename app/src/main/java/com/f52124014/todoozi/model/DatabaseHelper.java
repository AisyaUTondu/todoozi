package com.f52124014.todoozi.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.f52124014.todoozi.util.PasswordUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lapisan data lokal (SQLite).  (Modul 4)
 * - tabel users   : akun untuk mode lokal (password disimpan dalam bentuk hash)
 * - tabel todoozi : catatan & tugas milik masing-masing user, plus kolom sinkronisasi server
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "todoozi.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    private static final String TABLE_TODOOZI = "todoozi";
    private static final String COL_ID = "id";
    private static final String COL_USER = "user_id";
    private static final String COL_SERVER_ID = "server_id";
    private static final String COL_TITLE = "title";
    private static final String COL_CONTENT = "content";
    private static final String COL_CATEGORY = "category";
    private static final String COL_IS_COMPLETED = "is_completed";
    private static final String COL_DUE_DATE = "due_date";
    private static final String COL_SYNC = "sync_status";

    public DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)");
        createTodooziTable(db);
    }

    private void createTodooziTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TODOOZI + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER + " INTEGER NOT NULL DEFAULT 0, " +
                COL_SERVER_ID + " INTEGER NOT NULL DEFAULT 0, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_CATEGORY + " TEXT, " +
                COL_IS_COMPLETED + " INTEGER DEFAULT 0, " +
                COL_DUE_DATE + " TEXT DEFAULT '', " +
                COL_SYNC + " INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE INDEX idx_todoozi_user ON " + TABLE_TODOOZI + " (" + COL_USER + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 -> v2: data catatan/tugas sekarang per-user dan punya kolom sinkronisasi.
        // Tabel users TIDAK dihapus supaya akun lama tetap bisa login.
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TODOOZI);
            createTodooziTable(db);
        }
    }

    // ==========================================
    // USER (LOGIN & REGISTER MODE LOKAL)
    // ==========================================

    /** @return id user baru, atau -1 jika username sudah dipakai. */
    public long addUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, PasswordUtil.hash(password));
        return db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    /** @return id user jika username & password cocok, selain itu -1. */
    public int checkUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID, COL_PASSWORD},
                COL_USERNAME + "=?", new String[]{username}, null, null, null);
        try {
            if (!cursor.moveToFirst()) return -1;
            int id = cursor.getInt(0);
            String stored = cursor.getString(1);

            if (PasswordUtil.isHashed(stored)) {
                return PasswordUtil.verify(password, stored) ? id : -1;
            }
            // Akun lama (versi sebelumnya) masih berupa teks biasa: cocokkan lalu ubah menjadi hash.
            if (stored != null && stored.equals(password)) {
                ContentValues values = new ContentValues();
                values.put(COL_PASSWORD, PasswordUtil.hash(password));
                db.update(TABLE_USERS, values, COL_USER_ID + "=?", new String[]{String.valueOf(id)});
                return id;
            }
            return -1;
        } finally {
            cursor.close();
        }
    }

    // ==========================================
    // CRUD CATATAN & TUGAS
    // ==========================================

    private ContentValues toValues(NoteTaskModel m) {
        ContentValues values = new ContentValues();
        values.put(COL_SERVER_ID, m.getServerId());
        values.put(COL_TITLE, m.getTitle());
        values.put(COL_CONTENT, m.getContent());
        values.put(COL_CATEGORY, m.getCategory());
        values.put(COL_IS_COMPLETED, m.isCompleted() ? 1 : 0);
        values.put(COL_DUE_DATE, m.getDueDate());
        values.put(COL_SYNC, m.getSyncStatus());
        return values;
    }

    public long insertToodozi(int userId, NoteTaskModel m) {
        ContentValues values = toValues(m);
        values.put(COL_USER, userId);
        return getWritableDatabase().insert(TABLE_TODOOZI, null, values);
    }

    /** Daftar item user per kategori (yang sudah ditandai hapus tidak ikut). */
    public List<NoteTaskModel> getToodozi(int userId, String category) {
        String orderBy = COL_IS_COMPLETED + " ASC, "
                + "CASE WHEN " + COL_DUE_DATE + " IS NULL OR " + COL_DUE_DATE + "='' THEN 1 ELSE 0 END ASC, "
                + COL_DUE_DATE + " ASC, " + COL_ID + " DESC";
        Cursor cursor = getReadableDatabase().query(TABLE_TODOOZI, null,
                COL_USER + "=? AND " + COL_CATEGORY + "=? AND " + COL_SYNC + "<>?",
                new String[]{String.valueOf(userId), category, String.valueOf(NoteTaskModel.SYNC_DELETED)},
                null, null, orderBy);
        return readAll(cursor);
    }

    /** Item yang belum sinkron (baru/berubah/dihapus). */
    public List<NoteTaskModel> getPending(int userId) {
        Cursor cursor = getReadableDatabase().query(TABLE_TODOOZI, null,
                COL_USER + "=? AND " + COL_SYNC + "<>?",
                new String[]{String.valueOf(userId), String.valueOf(NoteTaskModel.SYNC_SYNCED)},
                null, null, COL_ID + " ASC");
        return readAll(cursor);
    }

    public int updateToodozi(NoteTaskModel m) {
        return getWritableDatabase().update(TABLE_TODOOZI, toValues(m),
                COL_ID + "=?", new String[]{String.valueOf(m.getId())});
    }

    public void deleteToodozi(int id) {
        getWritableDatabase().delete(TABLE_TODOOZI, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void markDeleted(int id) {
        ContentValues values = new ContentValues();
        values.put(COL_SYNC, NoteTaskModel.SYNC_DELETED);
        getWritableDatabase().update(TABLE_TODOOZI, values, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void markSynced(int id, int serverId) {
        ContentValues values = new ContentValues();
        values.put(COL_SERVER_ID, serverId);
        values.put(COL_SYNC, NoteTaskModel.SYNC_SYNCED);
        getWritableDatabase().update(TABLE_TODOOZI, values, COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    /** Simpan data dari server: perbarui jika sudah ada (dan tidak ada perubahan lokal), atau tambah baru. */
    public void upsertFromServer(int userId, NoteTaskModel m) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_TODOOZI, new String[]{COL_ID, COL_SYNC},
                COL_USER + "=? AND " + COL_SERVER_ID + "=?",
                new String[]{String.valueOf(userId), String.valueOf(m.getServerId())},
                null, null, null);
        int localId = -1;
        int status = NoteTaskModel.SYNC_SYNCED;
        if (cursor.moveToFirst()) {
            localId = cursor.getInt(0);
            status = cursor.getInt(1);
        }
        cursor.close();

        m.setSyncStatus(NoteTaskModel.SYNC_SYNCED);
        if (localId == -1) {
            ContentValues values = toValues(m);
            values.put(COL_USER, userId);
            db.insert(TABLE_TODOOZI, null, values);
        } else if (status == NoteTaskModel.SYNC_SYNCED) {
            db.update(TABLE_TODOOZI, toValues(m), COL_ID + "=?", new String[]{String.valueOf(localId)});
        }
        // jika ada perubahan lokal yang belum terkirim, perubahan lokal dipertahankan
    }

    /** Hapus data lokal yang sudah sinkron tetapi tidak ada lagi di server (dihapus dari perangkat lain). */
    public void deleteSyncedNotIn(int userId, Set<Integer> serverIds) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(TABLE_TODOOZI, new String[]{COL_ID, COL_SERVER_ID},
                COL_USER + "=? AND " + COL_SERVER_ID + ">0 AND " + COL_SYNC + "=?",
                new String[]{String.valueOf(userId), String.valueOf(NoteTaskModel.SYNC_SYNCED)},
                null, null, null);
        List<Integer> toDelete = new ArrayList<>();
        while (cursor.moveToNext()) {
            if (!serverIds.contains(cursor.getInt(1))) toDelete.add(cursor.getInt(0));
        }
        cursor.close();
        for (int id : toDelete) deleteToodozi(id);
    }

    private List<NoteTaskModel> readAll(Cursor cursor) {
        List<NoteTaskModel> list = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                NoteTaskModel m = new NoteTaskModel(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_COMPLETED)) == 1,
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DUE_DATE)));
                m.setServerId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SERVER_ID)));
                m.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNC)));
                list.add(m);
            }
        } finally {
            cursor.close();
        }
        return list;
    }
}
