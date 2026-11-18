package com.f52124014.todoozi.model;

import java.util.Objects;

/**
 * Model data catatan (NOTE) dan tugas (TASK).  (Modul 1: enkapsulasi, constructor, method)
 */
public class NoteTaskModel {

    public static final String CATEGORY_NOTE = "NOTE";
    public static final String CATEGORY_TASK = "TASK";

    /** Status sinkronisasi dengan server. */
    public static final int SYNC_SYNCED = 0;   // sama dengan server
    public static final int SYNC_PENDING = 1;  // baru / berubah, belum terkirim
    public static final int SYNC_DELETED = 2;  // sudah dihapus lokal, menunggu dihapus di server

    private int id;            // id lokal (SQLite)
    private int serverId;      // id di server (0 = belum ada di server)
    private String title;
    private String content;
    private String category;
    private boolean isCompleted;
    private String dueDate;    // "yyyy-MM-dd" atau ""
    private int syncStatus = SYNC_PENDING;

    public NoteTaskModel() {
        this.dueDate = "";
    }

    // Constructor standar (dipakai Catatan)
    public NoteTaskModel(int id, String title, String content, String category) {
        this(id, title, content, category, false, "");
    }

    // Constructor lengkap (dipakai Tugas)
    public NoteTaskModel(int id, String title, String content, String category, boolean isCompleted, String dueDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.isCompleted = isCompleted;
        this.dueDate = dueDate == null ? "" : dueDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getServerId() { return serverId; }
    public void setServerId(int serverId) { this.serverId = serverId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public String getDueDate() { return dueDate == null ? "" : dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate == null ? "" : dueDate; }
    public int getSyncStatus() { return syncStatus; }
    public void setSyncStatus(int syncStatus) { this.syncStatus = syncStatus; }

    public boolean isNote() {
        return CATEGORY_NOTE.equalsIgnoreCase(category);
    }

    /** Salinan baru (dipakai saat edit agar objek lama di daftar tidak ikut berubah). */
    public NoteTaskModel copy() {
        NoteTaskModel c = new NoteTaskModel(id, title, content, category, isCompleted, dueDate);
        c.serverId = serverId;
        c.syncStatus = syncStatus;
        return c;
    }

    /** Dipakai DiffUtil untuk mengetahui apakah tampilan item perlu diperbarui. */
    public boolean isSameContent(NoteTaskModel o) {
        return o != null
                && Objects.equals(title, o.title)
                && Objects.equals(content, o.content)
                && Objects.equals(category, o.category)
                && isCompleted == o.isCompleted
                && Objects.equals(getDueDate(), o.getDueDate());
    }
}
