# Todoozi v2 – Panduan

## 1. Yang berubah dibanding versi sebelumnya

**Tampilan**
- Palet ungu lembut baru (terang & gelap) yang dipusatkan di `values/colors.xml`, `values-night/colors.xml`, dan `themes.xml`. Tidak ada lagi warna hex yang ditulis langsung di layout, sehingga mode gelap ikut otomatis.
- Toolbar + TabLayout (ikon + teks), kartu bergaris tipis, tag kategori berbentuk pil, penanda tugas terlambat, empty state, ProgressBar, dan swipe-to-delete dengan tombol Batalkan.
- Login/Register memakai kolom outlined, ikon tampilkan-password, dan loading. Logo dan ikon aplikasi diganti ke versi ungu.
- Tambah/Edit memakai bottom sheet dengan pemilih tanggal (DatePicker).

**Penerapan modul yang sebelumnya belum ada**
| Modul | Penerapan baru |
|---|---|
| 1 | Semua teks dipindah ke `strings.xml`; `BaseActivity` & `ToodoziListFragment` (pewarisan); `LoginActivity`/`RegisterActivity` mengimplementasikan `View.OnClickListener`; `onSaveInstanceState` di `MainActivity`; data `username` dikirim lewat Intent |
| 2 | Menu List/Grid kini benar-benar tampil (Toolbar dipasang sebagai ActionBar); `ListAdapter` + `DiffUtil` (animasi); pilihan List/Grid tersimpan di ViewModel |
| 3 | `ViewPager2` + `FragmentStateAdapter` + `TabLayoutMediator` menggantikan `ViewPager`/`FragmentPagerAdapter` yang deprecated |
| 4 | **`AuthViewModel`** (login, register, logout, cek sesi) + `AuthRepository`; operasi SQLite di background thread; hasil lewat LiveData; password di-hash (PBKDF2); auto-login; tombol Keluar; data per-user (`user_id`) |
| 5 | Retrofit + Gson + OkHttp logging, izin INTERNET, `ApiService`/`ApiConfig`, Glide (foto profil), ProgressBar, penanganan error & token kedaluwarsa |

**Bug yang diperbaiki**
- Centang tugas kini benar-benar tersimpan ke database (sebelumnya listener tidak pernah dipasang).
- Menu List/Grid yang tidak tampil.
- Semua akun melihat data yang sama (kini terpisah per akun).
- Password tidak lagi disimpan sebagai teks biasa (akun lama otomatis di-hash saat pertama login).
- `strings.xml` dan `colors.xml` sebelumnya tertukar isinya; sudah dirapikan.

**Fitur tambahan:** pencarian, tanggal tenggat via DatePicker, urut otomatis (belum selesai & tenggat terdekat di atas), pull-to-refresh, hapus + Batalkan, statistik di Profil, auto-login/logout.

> Catatan: database naik ke versi 2. Akun lama tetap ada, tetapi catatan/tugas contoh lama dihapus karena strukturnya berubah.

## 2. Menjalankan aplikasi (mode lokal, tanpa server)
Buka folder `Todoozi` di Android Studio → Sync Gradle → Run. Pada awalnya `AppConfig.USE_API = false`, jadi seluruh data tersimpan di SQLite perangkat. Gradle perlu internet untuk mengunduh dependensi baru (Retrofit, Glide, dll.).

## 3. Menyalakan server API

### a. Jalankan backend (folder `backend/`)
Butuh Node.js 18+.
```bash
cd backend
npm install
cp .env.example .env        # Windows: copy .env.example .env  → lalu isi JWT_SECRET dengan teks acak panjang
npm start                   # server berjalan di http://localhost:3000
```
Cek di browser: `http://localhost:3000` harus menampilkan `{"name":"Todoozi API","status":"ok"}`.

### b. Hubungkan aplikasi
Edit `app/src/main/java/com/f52124014/todoozi/util/AppConfig.java`:
```java
public static final boolean USE_API = true;
public static final String BASE_URL = "http://10.0.2.2:3000/api/";   // emulator
```
- Emulator: `10.0.2.2` = laptop kamu.
- HP asli (satu Wi-Fi dengan laptop): pakai IP laptop, mis. `http://192.168.1.10:3000/api/` dan izinkan port 3000 di firewall.
- Server online: `https://domain-kamu.com/api/` (HTTPS), lalu hapus `android:usesCleartextTraffic="true"` di `AndroidManifest.xml`.

Pada mode API, akun dibuat di server (bukan di SQLite). Data tetap ditulis ke SQLite dulu (offline-first), lalu dikirim ke server; jika offline, perubahan menunggu dan dikirim saat tarik-untuk-segarkan (pull-to-refresh) atau perubahan berikutnya.

### c. Daftar endpoint
| Method | URL | Keterangan |
|---|---|---|
| POST | `/api/auth/register` | `{username, password}` → `{token, user}` |
| POST | `/api/auth/login` | `{username, password}` → `{token, user}` |
| GET | `/api/auth/me` | profil (wajib token) |
| GET | `/api/todoozi?category=NOTE\|TASK` | daftar milik user |
| POST | `/api/todoozi` | `{title, content, category, is_completed, due_date}` |
| PUT | `/api/todoozi/{id}` | ubah / centang selesai |
| DELETE | `/api/todoozi/{id}` | hapus |

Token dikirim lewat header `Authorization: Bearer <token>`. `due_date` berformat `yyyy-MM-dd`. Error selalu berbentuk `{"message": "..."}`.

### d. Menaruh server di internet
Backend ini siap di-deploy ke layanan hosting Node.js (mis. Render, Railway, atau VPS). Atur environment variable `JWT_SECRET`, `PORT`, dan `DB_FILE`. Database SQLite berupa satu file, jadi pastikan hosting punya penyimpanan (disk) yang persisten; jika tidak, data hilang saat server restart. Untuk penggunaan serius, pindahkan ke PostgreSQL.

## 4. Struktur kode
```
com.f52124014.todoozi
├── model/        NoteTaskModel, DatabaseHelper (SQLite v2)
├── network/      ApiService, ApiConfig (Retrofit), DTO
├── repository/   AuthRepository, ToodoziRepository (offline-first + sinkronisasi)
├── viewmodel/    AuthViewModel, ToodoziViewModel, AuthResult
├── util/         AppConfig, SessionManager, PasswordUtil, DateUtil, Event
└── view/         BaseActivity, Login/Register/MainActivity, TodoEditorSheet
    ├── adapter/  SectionsPagerAdapter, ToodoziAdapter
    └── fragment/ ToodoziListFragment (induk), Notes/Tasks/ProfileFragment
```
