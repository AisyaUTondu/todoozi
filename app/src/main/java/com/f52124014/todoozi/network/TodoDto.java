package com.f52124014.todoozi.network;

import com.f52124014.todoozi.model.NoteTaskModel;
import com.google.gson.annotations.SerializedName;

/** Representasi JSON satu catatan/tugas di server. */
public class TodoDto {
    private int id;
    private String title;
    private String content;
    private String category;
    @SerializedName("is_completed")
    private boolean isCompleted;
    @SerializedName("due_date")
    private String dueDate;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public boolean isCompleted() { return isCompleted; }
    public String getDueDate() { return dueDate; }

    public static TodoDto fromModel(NoteTaskModel m) {
        TodoDto dto = new TodoDto();
        dto.id = m.getServerId();
        dto.title = m.getTitle();
        dto.content = m.getContent();
        dto.category = m.getCategory();
        dto.isCompleted = m.isCompleted();
        dto.dueDate = m.getDueDate();
        return dto;
    }

    /** Hasil konversi berstatus SYNCED; id lokal diisi 0 (ditentukan oleh SQLite). */
    public NoteTaskModel toModel() {
        NoteTaskModel m = new NoteTaskModel(0, title, content == null ? "" : content,
                category, isCompleted, dueDate == null ? "" : dueDate);
        m.setServerId(id);
        m.setSyncStatus(NoteTaskModel.SYNC_SYNCED);
        return m;
    }
}
