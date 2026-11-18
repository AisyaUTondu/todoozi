package com.f52124014.todoozi.util;

/**
 * Pembungkus data LiveData yang hanya boleh dikonsumsi sekali (mis. pesan Toast / navigasi),
 * agar tidak muncul lagi saat layar diputar.
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /** Mengembalikan isi hanya pada pemanggilan pertama, setelah itu null. */
    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}
