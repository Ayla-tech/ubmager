# Panduan SQL Supabase — Modul Pemesanan & Pembayaran

Folder ini berisi SQL yang dijalankan **manual** melalui Supabase SQL Editor pada
project Supabase pengembangan Anda. Aplikasi **tidak** membuat atau mengubah tabel
secara otomatis (`spring.jpa.hibernate.ddl-auto=none`).

> Jangan menjalankan SQL ini pada database kelompok. Buat project Supabase terpisah
> untuk modul ini.

## 1. Prasyarat tabel

Tiga tabel berikut harus sudah ada **sebelum** file `01`–`04` dijalankan.
Struktur di bawah adalah kolom minimum yang dipakai modul ini.

### `users`

| Kolom | Tipe | Keterangan |
|---|---|---|
| id_user | VARCHAR(10) | Primary key |
| username | VARCHAR(50) | Unik |
| email_atau_hp | VARCHAR(100) | Unik |
| password | VARCHAR(255) | Tidak dibaca modul ini |
| status_akun | VARCHAR(20) | Tidak dibaca modul ini |
| created_at | TIMESTAMP | Tidak dibaca modul ini |
| update_at | TIMESTAMP | Tidak dibaca modul ini |
| rata_rata_rating | REAL | Tidak dibaca modul ini |
| badge_kepercayaan | VARCHAR(30) | Tidak dibaca modul ini |

### `postingan`

Entity/CRUD postingan adalah tanggung jawab modul lain. Modul ini hanya membaca
empat kolom berikut:

| Kolom | Tipe | Keterangan |
|---|---|---|
| id_postingan | VARCHAR(10) | Primary key |
| id_penjual | VARCHAR(10) | Pemilik/pengiklan jasa, mengacu ke `users.id_user` |
| harga | NUMERIC(12,2) | Harga satuan jasa |
| status_aktif | BOOLEAN | `true` berarti postingan masih aktif |

Catatan penting:

- Nama tabel dan kolom di atas mengikuti brief modul. Jika modul Postingan kelompok
  memakai nama berbeda (misalnya `status` bertipe teks), sesuaikan **satu** query di
  `PostinganBacaRepository` dan constraint FK di `02_pemesanan.sql`.
- Jangan membuat tabel `postingan` dari folder ini. Jika tabel belum ada, koordinasikan
  dengan pemilik modul Postingan.
- Jika `postingan.id_postingan` bukan `VARCHAR(10)`, samakan tipe kolomnya sebelum
  menambahkan foreign key.

### `pesanan`

Kolom yang wajib sudah ada:

| Kolom | Tipe | Keterangan |
|---|---|---|
| id_pesanan | VARCHAR(10) | Primary key |
| id_pembeli | VARCHAR(10) | Mengacu ke `users.id_user` |
| id_postingan | VARCHAR(10) | Mengacu ke `postingan.id_postingan` |
| status_pesanan | VARCHAR(15) | Nilai: menunggu, dibayar, selesai, dibatalkan |

Dua kolom tambahan (`tanggal_pesan`, `total_harga`) ditambahkan oleh `01`.

## 2. Urutan eksekusi

1. `01_pesanan_tambahan_kolom.sql`
2. `02_pemesanan.sql`
3. `03_pembayaran.sql`
4. `04_datapembayaran.sql`
5. `05_verifikasi.sql` (hanya SELECT, aman dijalankan kapan saja)

## 3. Backfill data lama

### 3.1 `tanggal_pesan`

`01` **tidak** mengisi `tanggal_pesan` data lama, dan kolomnya tetap **NULLABLE**.
Tanggal migrasi (`NOW()`) tidak boleh dipakai karena itu bukan tanggal pesanan
yang sebenarnya.

Langkah manual:

1. Cari baris yang belum terisi:

   ```sql
   SELECT id_pesanan, id_pembeli, id_postingan, status_pesanan
   FROM pesanan
   WHERE tanggal_pesan IS NULL;
   ```

2. Isi dari sumber data asli (arsip transaksi, sistem lama, atau konfirmasi
   pemilik data). Contoh pola, sesuaikan nama tabel/kolom sumber:

   ```sql
   UPDATE pesanan p
   SET tanggal_pesan = s.tanggal_transaksi
   FROM sumber_data_asli s
   WHERE s.id_pesanan = p.id_pesanan
     AND p.tanggal_pesan IS NULL;
   ```

3. Jalankan `ALTER TABLE pesanan ALTER COLUMN tanggal_pesan SET NOT NULL;`
   **hanya setelah** query pada langkah 1 menghasilkan 0 baris.

Pesanan baru dari `POST /api/pemesanan` selalu mengisi `tanggal_pesan` otomatis
dari aplikasi (`LocalDateTime.now()`).

### 3.2 `total_harga`

`01` **tidak** mengisi `total_harga` agar tidak ada nominal buatan. Untuk data
`pesanan` lama:

1. Jalankan query di `05_verifikasi.sql` bagian "Cek data yang belum memiliki
   total_harga".
2. Isi setiap baris dari sumber data yang benar (misalnya rekap transaksi).
3. Setelah semua baris terisi, buka komentar:

   ```sql
   ALTER TABLE pesanan ALTER COLUMN total_harga SET NOT NULL;
   ```

Pesanan baru dari endpoint `POST /api/pemesanan` selalu mengisi `total_harga`
otomatis dari `postingan.harga × jumlah`.

### 3.3 Default status

DDL di folder ini memberi nilai awal untuk status:

```sql
-- 02_pemesanan.sql
status_pesanan VARCHAR(20) NOT NULL DEFAULT 'menunggu'

-- 03_pembayaran.sql
status        VARCHAR(30) NOT NULL DEFAULT 'menunggu_konfirmasi'
```

Aplikasi tetap mengisi status secara eksplisit; DEFAULT hanya pengaman untuk
insert manual. Constraint tetap membatasi nilai:

| Tabel | Nilai yang diizinkan |
|---|---|
| `pesanan` | `menunggu`, `dibayar`, `selesai`, `dibatalkan` |
| `pemesanan` | `menunggu`, `diterima`, `ditolak` |
| `pembayaran` | `menunggu_konfirmasi`, `perbaikan`, `dikonfirmasi` |

## 4. Konfigurasi aplikasi

Buat project Supabase, lalu isi file `.env` lokal (jangan di-commit):

```properties
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=<username>
SUPABASE_DB_PASSWORD=<password>
```

Nama variabel dipertahankan dari konfigurasi kelompok. `application.properties`
sudah membaca ketiga variabel tersebut. Jangan menulis kredensial ke kode atau
dokumen.

## 5. Verifikasi cepat

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN ('pesanan', 'pemesanan', 'pembayaran', 'datapembayaran');
```

Empat baris harus muncul. Lalu jalankan `05_verifikasi.sql`.
