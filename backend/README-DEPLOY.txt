TODOOZI BACKEND UNTUK DEPLOY KE RENDER + SUPABASE

A. Pasang backend
1. Backup folder backend lama.
2. Ganti isi folder Todoozi/backend dengan isi folder backend pada paket ini.
   Jangan ikut menyalin .env.example menjadi .env.
3. Pastikan routes.js tetap ada (sudah disertakan).
4. Jangan unggah .env, password database, atau secret ke GitHub.

B. Pastikan tabel Supabase
- Tabel public.users: id, username, password_hash, created_at
- Tabel public.todoozi: id, user_id, title, content, category,
  is_completed, due_date, created_at, updated_at
- Bila tabel belum ada, jalankan supabase-schema.sql di SQL Editor.
- Jika tabel sudah ada, jangan jalankan SQL secara membabi buta; cek kolomnya
  terlebih dahulu di Table Editor. Script ini mengasumsikan is_completed BOOLEAN
  dan due_date DATE (atau kolom yang kompatibel).

C. Uji lokal
1. Di backend/.env, isi DATABASE_URL dengan Supabase Session Pooler URI
   dan JWT_SECRET acak minimal 32 karakter.
2. Dari terminal backend:
      npm install
      npm start
3. Buka http://localhost:3000/ dan http://localhost:3000/health
   Health seharusnya menampilkan {"status":"ok","database":"connected"}.

D. Deploy ke Render (butuh akun Render dan repo GitHub milik pengguna)
1. Unggah project Todoozi ke repository GitHub. Pastikan backend/.env tidak
   masuk commit; .gitignore sudah mengabaikannya.
2. Render Dashboard > New > Web Service > hubungkan repository.
3. Root Directory: backend
4. Build Command: npm install
5. Start Command: npm start
6. Environment Variables:
   DATABASE_URL = URI Session Pooler Supabase (jangan dibagikan)
   JWT_SECRET = string acak panjang minimal 32 karakter
   JWT_EXPIRES_IN = 7d
   NODE_ENV = production
   AVATAR_URL_TEMPLATE = https://ui-avatars.com/api/?name={username}&background=6C4FD1&color=ffffff&size=256&format=png
7. Deploy. Setelah Live, buka https://NAMA-SERVICE.onrender.com/health.
   Harus tampil database connected.

E. Sambungkan Android
Edit file:
app/src/main/java/com/f52124014/todoozi/util/AppConfig.java

Pertahankan USE_API = true dan ubah BASE_URL menjadi:
public static final String BASE_URL = "https://NAMA-SERVICE.onrender.com/api/";

Ganti NAMA-SERVICE dengan hostname Render yang benar, tanpa / di akhir domain
sebelum /api/. Build ulang aplikasi dan install di HP.

F. Mulai benar-benar dari akun kosong
- Logout dari aplikasi.
- Uninstall aplikasi Todoozi dari HP lalu install ulang APK terbaru untuk
  membersihkan database lokal dan token lama. Ini menghapus data lokal di HP.
- Daftarkan akun baru, buat catatan dan tugas.
- Cek Table Editor Supabase: users dan todoozi.

CATATAN
- Saya tidak dapat membuat service Render atau mengubah repository GitHub milikmu
  tanpa akses/login yang kamu lakukan sendiri.
- Render free service dapat sleep saat tidak dipakai dan request pertama setelah
  idle bisa lebih lambat. Supabase tetap menyimpan data cloud.
- Jangan pernah mengirim .env atau URI database ke chat.
