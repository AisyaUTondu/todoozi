package com.f52124014.todoozi.util;

/**
 * Pengaturan global aplikasi.
 *
 * USE_API = false  -> aplikasi berjalan penuh secara lokal (SQLite) tanpa server.
 * USE_API = true   -> login/register dan data catatan/tugas disinkronkan ke server REST API
 *                     (lihat folder backend/ di root proyek).
 */
public final class AppConfig {

    private AppConfig() {}

    /** Ubah ke true setelah server berjalan. */
    public static final boolean USE_API = true;

    /**
     * Alamat server. Wajib diakhiri tanda "/".
     * - Emulator Android Studio : http://10.0.2.2:3000/api/
     * - HP asli (satu Wi-Fi)    : http://IP-LAPTOP:3000/api/   (contoh http://192.168.1.10:3000/api/)
     * - Server online           : https://domain-kamu.com/api/
     */
    public static final String BASE_URL = "http://127.0.0.1:3000/api/";
}
