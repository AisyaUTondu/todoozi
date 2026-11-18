package com.f52124014.todoozi.repository;

import android.content.Context;

import com.f52124014.todoozi.R;
import com.f52124014.todoozi.model.DatabaseHelper;
import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.network.ApiConfig;
import com.f52124014.todoozi.network.ApiHelper;
import com.f52124014.todoozi.network.ApiService;
import com.f52124014.todoozi.network.MessageResponse;
import com.f52124014.todoozi.network.TodoDto;
import com.f52124014.todoozi.network.TodoListResponse;
import com.f52124014.todoozi.network.TodoResponse;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Response;

/**
 * Repository catatan & tugas (offline-first).
 * Semua perubahan ditulis ke SQLite lebih dulu (cepat, tetap jalan tanpa internet), lalu dikirim
 * ke server oleh pushPending()/sync(). Semua method di sini HARUS dipanggil dari background thread.
 */
public class ToodoziRepository {

    /** Hasil sinkronisasi dengan server. */
    public static class SyncResult {
        public final boolean unauthorized;
        public final String error;

        private SyncResult(boolean unauthorized, String error) {
            this.unauthorized = unauthorized;
            this.error = error;
        }

        static SyncResult ok() { return new SyncResult(false, null); }

        static SyncResult unauthorized() { return new SyncResult(true, null); }

        static SyncResult error(String message) { return new SyncResult(false, message); }
    }

    private final Context context;
    private final DatabaseHelper db;

    public ToodoziRepository(Context context) {
        this.context = context.getApplicationContext();
        this.db = new DatabaseHelper(this.context);
    }

    // ---------------- lokal ----------------

    public List<NoteTaskModel> getLocal(int userId, String category) {
        return db.getToodozi(userId, category);
    }

    public void addLocal(int userId, NoteTaskModel item) {
        item.setServerId(0);
        item.setSyncStatus(NoteTaskModel.SYNC_PENDING);
        db.insertToodozi(userId, item);
    }

    public void updateLocal(NoteTaskModel item) {
        item.setSyncStatus(NoteTaskModel.SYNC_PENDING);
        db.updateToodozi(item);
    }

    public void deleteLocal(NoteTaskModel item) {
        if (item.getServerId() == 0) {
            db.deleteToodozi(item.getId());   // belum pernah sampai ke server
        } else {
            db.markDeleted(item.getId());     // hapus di server dulu, baru hapus permanen
        }
    }

    // ---------------- sinkronisasi ----------------

    /** Kirim semua perubahan lokal, lalu ambil data terbaru dari server. */
    public SyncResult sync(int userId) {
        SyncResult push = pushPending(userId);
        if (push.unauthorized) return push;

        ApiService api = ApiConfig.getService(context);
        try {
            Response<TodoListResponse> response = api.getTodoozi().execute();
            if (response.code() == 401) return SyncResult.unauthorized();
            if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                return SyncResult.error(ApiHelper.errorMessage(response));
            }
            Set<Integer> serverIds = new HashSet<>();
            for (TodoDto dto : response.body().getData()) {
                serverIds.add(dto.getId());
                db.upsertFromServer(userId, dto.toModel());
            }
            db.deleteSyncedNotIn(userId, serverIds);
            return push.error != null ? push : SyncResult.ok();
        } catch (IOException e) {
            return SyncResult.error(context.getString(R.string.msg_offline_local));
        }
    }

    /** Kirim item yang baru / diubah / dihapus ke server. */
    public SyncResult pushPending(int userId) {
        ApiService api = ApiConfig.getService(context);
        String firstError = null;

        for (NoteTaskModel item : db.getPending(userId)) {
            try {
                if (item.getSyncStatus() == NoteTaskModel.SYNC_DELETED) {
                    if (item.getServerId() == 0) {
                        db.deleteToodozi(item.getId());
                        continue;
                    }
                    Response<MessageResponse> r = api.deleteTodoozi(item.getServerId()).execute();
                    if (r.code() == 401) return SyncResult.unauthorized();
                    if (r.isSuccessful() || r.code() == 404) {
                        db.deleteToodozi(item.getId());
                    } else if (firstError == null) {
                        firstError = ApiHelper.errorMessage(r);
                    }
                } else {
                    TodoDto body = TodoDto.fromModel(item);
                    Response<TodoResponse> r = item.getServerId() == 0
                            ? api.createTodoozi(body).execute()
                            : api.updateTodoozi(item.getServerId(), body).execute();
                    if (r.code() == 401) return SyncResult.unauthorized();
                    if (r.isSuccessful() && r.body() != null && r.body().getData() != null) {
                        db.markSynced(item.getId(), r.body().getData().getId());
                    } else if (firstError == null) {
                        firstError = ApiHelper.errorMessage(r);
                    }
                }
            } catch (IOException e) {
                return SyncResult.error(context.getString(R.string.msg_offline_local));
            }
        }
        return firstError == null ? SyncResult.ok() : SyncResult.error(firstError);
    }
}
