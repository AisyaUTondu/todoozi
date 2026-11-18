package com.f52124014.todoozi.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.f52124014.todoozi.model.NoteTaskModel;
import com.f52124014.todoozi.repository.ToodoziRepository;
import com.f52124014.todoozi.util.AppConfig;
import com.f52124014.todoozi.util.Event;
import com.f52124014.todoozi.util.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel bersama untuk Catatan, Tugas, dan Profil (Modul 3 & 4).
 * Semua akses database / jaringan berjalan di satu background thread.
 */
public class ToodoziViewModel extends AndroidViewModel {

    private final ToodoziRepository repository;
    private final SessionManager session;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<NoteTaskModel>> allNotes = new MutableLiveData<>();
    private final MutableLiveData<List<NoteTaskModel>> allTasks = new MutableLiveData<>();
    private final MediatorLiveData<List<NoteTaskModel>> notes = new MediatorLiveData<>();
    private final MediatorLiveData<List<NoteTaskModel>> tasks = new MediatorLiveData<>();

    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> gridMode = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> sessionExpired = new MutableLiveData<>();

    public ToodoziViewModel(@NonNull Application application) {
        super(application);
        repository = new ToodoziRepository(application);
        session = new SessionManager(application);

        // Daftar yang tampil = data lengkap disaring kata kunci pencarian
        notes.addSource(allNotes, list -> notes.setValue(filter(list, query.getValue())));
        notes.addSource(query, q -> {
            List<NoteTaskModel> source = allNotes.getValue();
            if (source != null) notes.setValue(filter(source, q));
        });
        tasks.addSource(allTasks, list -> tasks.setValue(filter(list, query.getValue())));
        tasks.addSource(query, q -> {
            List<NoteTaskModel> source = allTasks.getValue();
            if (source != null) tasks.setValue(filter(source, q));
        });
    }

    // ---------------- LiveData untuk UI ----------------

    public LiveData<List<NoteTaskModel>> getNotes() { return notes; }

    public LiveData<List<NoteTaskModel>> getTasks() { return tasks; }

    /** Data lengkap tanpa filter pencarian (dipakai statistik di Profil). */
    public LiveData<List<NoteTaskModel>> getAllNotes() { return allNotes; }

    public LiveData<List<NoteTaskModel>> getAllTasks() { return allTasks; }

    public LiveData<Boolean> getGridMode() { return gridMode; }

    public LiveData<Boolean> getLoading() { return loading; }

    public LiveData<Event<String>> getMessage() { return message; }

    public LiveData<Event<Boolean>> getSessionExpired() { return sessionExpired; }

    public void setGridMode(boolean grid) { gridMode.setValue(grid); }

    public void setQuery(String text) {
        String clean = text == null ? "" : text;
        if (!clean.equals(query.getValue())) query.setValue(clean);
    }

    public String getQueryValue() {
        String q = query.getValue();
        return q == null ? "" : q;
    }

    // ---------------- aksi ----------------

    /** Muat data lokal, lalu (jika USE_API) sinkronkan dengan server. */
    public void refresh() {
        loading.setValue(true);
        executor.execute(() -> {
            publishLocal();
            if (AppConfig.USE_API) {
                ToodoziRepository.SyncResult result = repository.sync(session.getUserId());
                publishLocal();
                if (result.unauthorized) {
                    sessionExpired.postValue(new Event<>(true));
                } else if (result.error != null) {
                    message.postValue(new Event<>(result.error));
                }
            }
            loading.postValue(false);
        });
    }

    public void addToodozi(NoteTaskModel item) {
        executor.execute(() -> {
            repository.addLocal(session.getUserId(), item);
            publishLocal();
            pushChanges();
        });
    }

    public void updateToodozi(NoteTaskModel item) {
        executor.execute(() -> {
            repository.updateLocal(item);
            publishLocal();
            pushChanges();
        });
    }

    public void setCompleted(NoteTaskModel item, boolean completed) {
        NoteTaskModel copy = item.copy();
        copy.setCompleted(completed);
        updateToodozi(copy);
    }

    public void deleteToodozi(NoteTaskModel item) {
        executor.execute(() -> {
            repository.deleteLocal(item);
            publishLocal();
            pushChanges();
        });
    }

    /** Batalkan penghapusan: buat ulang item sebagai data baru. */
    public void restoreToodozi(NoteTaskModel item) {
        NoteTaskModel copy = item.copy();
        copy.setId(0);
        copy.setServerId(0);
        addToodozi(copy);
    }

    // ---------------- internal ----------------

    private void publishLocal() {
        int userId = session.getUserId();
        allNotes.postValue(repository.getLocal(userId, NoteTaskModel.CATEGORY_NOTE));
        allTasks.postValue(repository.getLocal(userId, NoteTaskModel.CATEGORY_TASK));
    }

    /** Kirim perubahan ke server secara diam-diam; jika gagal akan dicoba lagi saat refresh. */
    private void pushChanges() {
        if (!AppConfig.USE_API) return;
        ToodoziRepository.SyncResult result = repository.pushPending(session.getUserId());
        publishLocal();
        if (result.unauthorized) sessionExpired.postValue(new Event<>(true));
    }

    private static List<NoteTaskModel> filter(List<NoteTaskModel> source, String query) {
        if (source == null || query == null || query.trim().isEmpty()) return source;
        String needle = query.trim().toLowerCase(Locale.getDefault());
        List<NoteTaskModel> out = new ArrayList<>();
        for (NoteTaskModel item : source) {
            if (contains(item.getTitle(), needle) || contains(item.getContent(), needle)) out.add(item);
        }
        return out;
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.getDefault()).contains(needle);
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
    }
}
