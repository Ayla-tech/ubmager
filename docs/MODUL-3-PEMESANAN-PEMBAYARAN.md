# Modul Pemesanan & Pembayaran — UBMager

Implementasi di branch `iqbal`, mengikuti pola controller/repository/`Hasil` milik
kelompok. Tidak ada service layer, tidak ada dependency baru selain H2 untuk
pengujian lokal.

## 1. Ringkasan hasil

- Entity: `Pemesanan`, `Pembayaran`, `DataPembayaran`; `Pesanan` ditambah
  `tanggal_pesan` dan `total_harga`.
- Repository: `PemesananRepository`, `PembayaranRepository`,
  `DataPembayaranRepository`; `PesananRepository` ditambah query daftar.
- Controller: `PemesananController` (`/api/pemesanan`) dan
  `PembayaranController` (`/api/pembayaran`).
- Integrasi Postingan: `PostinganBacaRepository` (native query baca-saja).
- SQL Supabase: folder `database/supabase/`.
- UI uji manual: dua bagian baru di `templates/index.html`.
- Pengujian otomatis: 36 test modul + 1 test context, semuanya lulus dengan H2
  in-memory. Database kelompok/Supabase tidak disentuh.

## 2. File yang dibuat

| File | Isi |
|---|---|
| `entity/Pemesanan.java` | Mapping tabel `pemesanan` |
| `entity/Pembayaran.java` | Mapping tabel `pembayaran` |
| `entity/DataPembayaran.java` | Mapping tabel `datapembayaran` |
| `repository/PemesananRepository.java` | `JpaRepository` + query pembeli/postingan |
| `repository/PembayaranRepository.java` | `findByIdPesanan`, `existsByIdPesanan` |
| `repository/DataPembayaranRepository.java` | Riwayat bukti per pembayaran |
| `repository/PostinganBacaRepository.java` | Baca `id_postingan`, `id_penjual`, `harga`, `status_aktif` |
| `controller/PemesananController.java` | Seluruh endpoint pemesanan |
| `controller/PembayaranController.java` | Seluruh endpoint pembayaran |
| `database/supabase/01_pesanan_tambahan_kolom.sql` | `tanggal_pesan`, `total_harga`, backfill, check status |
| `database/supabase/02_pemesanan.sql` | Tabel + constraint |
| `database/supabase/03_pembayaran.sql` | Tabel + constraint |
| `database/supabase/04_datapembayaran.sql` | Tabel + constraint |
| `database/supabase/05_verifikasi.sql` | Query verifikasi read-only |
| `database/supabase/README.md` | Prasyarat, urutan eksekusi, env, backfill |
| `src/test/resources/application.properties` | Konfigurasi H2 untuk test |
| `src/test/resources/schema-test.sql` | Skema uji setara DDL Supabase |
| `src/test/resources/demo-seed.sql` | Data awal untuk demo HTTP lokal (USR001, USR002, PST001) |
| `src/test/java/.../PemesananControllerTest.java` | 20 test pemesanan |
| `src/test/java/.../PembayaranControllerTest.java` | 16 test pembayaran |
| `docs/MODUL-3-PEMESANAN-PEMBAYARAN.md` | Dokumen ini |

## 3. File yang diubah

| File | Perubahan |
|---|---|
| `entity/Pesanan.java` | Tambah `tanggalPesan` (`LocalDateTime`), `totalHarga` (`BigDecimal`); kolom lama tidak diubah |
| `repository/PesananRepository.java` | Tambah `findByIdPembeliOrderByTanggalPesanDesc` |
| `pom.xml` | Tambah `com.h2database:h2` scope `test` |
| `templates/index.html` | Tambah bagian 4 (Pemesanan) dan 5 (Pembayaran) |

`ProfilController`, `ReviewController`, dan entity/repository lain tidak diubah.

## 4. Entity dan relasi

```
USERS 1 ──── * PEMESANAN * ──── 1 POSTINGAN
                  │ 1
                  │
                  1
PESANAN 1 ──── 1 PEMESANAN
PESANAN 1 ──── 0..1 PEMBAYARAN
PEMBAYARAN 1 ──── * DATAPEMBAYARAN
```

- `PESANAN` = header transaksi (dipakai ReviewController).
- `PEMESANAN` = detail pemesanan jasa, PK `id_pemesanan` (prefix `PMS`), FK unik
  `id_pesanan` ke `pesanan`.
- `PEMBAYARAN` = satu pembayaran per pesanan, PK `PBY`, FK unik `id_pesanan`.
- `DATAPEMBAYARAN` = riwayat bukti, PK `DPY`, boleh banyak per pembayaran.

Nilai status:

| Tabel | Nilai |
|---|---|
| `pesanan.status_pesanan` | `menunggu`, `dibayar`, `selesai`, `dibatalkan` |
| `pemesanan.status_pesanan` | `menunggu`, `diterima`, `ditolak` |
| `pembayaran.status` | `menunggu_konfirmasi`, `perbaikan`, `dikonfirmasi` |

## 5. Endpoint

### Pemesanan

| Method | Endpoint | Fungsi |
|---|---|---|
| POST | `/api/pemesanan` | Buat pesanan + pemesanan dalam satu transaksi |
| GET | `/api/pemesanan/{idPemesanan}` | Detail pemesanan + pesanan |
| GET | `/api/pemesanan/pembeli/{idPembeli}` | Daftar pemesanan pembeli |
| GET | `/api/pemesanan/penyedia/{idPenyedia}` | Daftar pemesanan berdasarkan postingan milik penyedia |
| PUT | `/api/pemesanan/{idPemesanan}/terima` | Ubah pemesanan menjadi `diterima` |
| PUT | `/api/pemesanan/{idPemesanan}/tolak` | Pemesanan `ditolak`, pesanan `dibatalkan` |
| POST | `/api/pemesanan/{idPemesanan}/selesai` | Pesanan `selesai` setelah pembayaran dikonfirmasi |

### Pembayaran

| Method | Endpoint | Fungsi |
|---|---|---|
| POST | `/api/pembayaran` | Buat pembayaran dari `pesanan.total_harga` |
| GET | `/api/pembayaran/{idPembayaran}` | Detail pembayaran + riwayat bukti + pesanan |
| GET | `/api/pembayaran/pesanan/{idPesanan}` | Pembayaran berdasarkan pesanan |
| POST | `/api/pembayaran/{idPembayaran}/bukti` | Simpan URL/path bukti |
| PUT | `/api/pembayaran/{idPembayaran}/konfirmasi` | Pembayaran `dikonfirmasi`, pesanan `dibayar` |
| PUT | `/api/pembayaran/{idPembayaran}/perbaikan` | Pembayaran `perbaikan`, pesanan tetap |

Semua respons memakai record `Hasil`:

```json
{ "berhasil": true, "pesan": "...", "data": { } }
```

Gagal: pesan mengandung `"tidak ditemukan"` → HTTP 404, selain itu HTTP 400.
Sukses: 201 untuk create, 200 untuk read/update.

## 6. Contoh request dan respons

### 6.1 Buat pemesanan

```http
POST /api/pemesanan
Content-Type: application/json
```

```json
{
  "id_pembeli": "USR001",
  "id_postingan": "PST001",
  "jumlah": 2,
  "waktu_pesanan": "2026-12-01 10:00:00",
  "lokasi": "Jl. Contoh No. 1",
  "catatan": "Tolong datang tepat waktu"
}
```

```json
{
  "berhasil": true,
  "pesan": "Pemesanan berhasil dibuat",
  "data": {
    "pemesanan": {
      "id_pemesanan": "PMS001",
      "id_pesanan": "PSN001",
      "id_postingan": "PST001",
      "id_pembeli": "USR001",
      "jumlah": 2,
      "waktu_pesanan": "2026-12-01T10:00:00",
      "lokasi": "Jl. Contoh No. 1",
      "catatan": "Tolong datang tepat waktu",
      "status_pesanan": "menunggu",
      "created_at": "2026-11-19T21:39:45"
    },
    "pesanan": {
      "id_pesanan": "PSN001",
      "id_pembeli": "USR001",
      "id_postingan": "PST001",
      "status_pesanan": "menunggu",
      "tanggal_pesan": "2026-11-19T21:39:45",
      "total_harga": 100000.00
    }
  }
}
```

`total_harga` dihitung dari `postingan.harga × jumlah`. Nilai `total_harga` pada
request diabaikan.

### 6.2 Terima / tolak / selesai

```json
{ "id_penyedia": "USR002" }
```

`id_penyedia` **wajib** dikirim pada terima, tolak, selesai, konfirmasi, dan
perbaikan. Nilainya harus sama dengan `postingan.id_penjual`. Bila kosong atau
tidak dikirim, request gagal 400 dengan pesan `"ID penyedia wajib dikirim"`.
Ini belum autentikasi penuh karena identitas masih berasal dari request.

### 6.3 Buat pembayaran

```json
{ "id_pesanan": "PSN001", "metode": "transfer" }
```

Nominal diambil dari `pesanan.total_harga`; field `jumlah` pada request tidak
dipakai.

### 6.4 Simpan bukti

```json
{ "bukti_pembayaran": "https://contoh.test/bukti.jpg" }
```

Bila status pembayaran `perbaikan`, status otomatis kembali ke
`menunggu_konfirmasi` setelah bukti baru tersimpan, dan bukti lama tetap tersimpan
sebagai riwayat.

### 6.5 Konfirmasi / minta perbaikan

```json
{ "id_penyedia": "USR002" }
```

## 7. Aturan transisi status

| Aksi | Syarat | Akibat |
|---|---|---|
| Buat pemesanan | pembeli ada, postingan ada & aktif, jumlah > 0 | PESANAN `menunggu`, PEMESANAN `menunggu` |
| Terima | PEMESANAN `menunggu` | PEMESANAN `diterima`, PESANAN tetap `menunggu` |
| Tolak | PEMESANAN `menunggu`, belum ada pembayaran | PEMESANAN `ditolak`, PESANAN `dibatalkan` |
| Selesai | PEMESANAN `diterima`, pembayaran `dikonfirmasi` | PESANAN `selesai`, PEMESANAN tetap `diterima` |
| Buat pembayaran | PEMESANAN `diterima`, PESANAN `menunggu`, belum ada pembayaran | PEMBAYARAN `menunggu_konfirmasi` |
| Konfirmasi | PEMBAYARAN `menunggu_konfirmasi`, PESANAN `menunggu` | PEMBAYARAN `dikonfirmasi`, PESANAN `dibayar` |
| Perbaikan | PEMBAYARAN `menunggu_konfirmasi` | PEMBAYARAN `perbaikan`, PESANAN tidak berubah |

## 8. SQL Supabase

Jalankan manual melalui SQL Editor, berurutan:

1. `database/supabase/01_pesanan_tambahan_kolom.sql`
2. `database/supabase/02_pemesanan.sql`
3. `database/supabase/03_pembayaran.sql`
4. `database/supabase/04_datapembayaran.sql`
5. `database/supabase/05_verifikasi.sql`

Prasyarat lengkap (kolom `users`, `postingan`, `pesanan`), urutan, dan langkah
backfill `total_harga` ada di `database/supabase/README.md`.

Environment variable (tetap memakai nama milik kelompok):

```properties
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=<username>
SUPABASE_DB_PASSWORD=<password>
```

`ddl-auto` tetap `none`. Aplikasi tidak membuat tabel otomatis.

## 9. Cara menjalankan

```powershell
# 1. Isi .env lokal (jangan commit)
# 2. Build dan jalankan
.\mvnw.cmd package
.\mvnw.cmd spring-boot:run
```

Buka `http://localhost:8080/` untuk UI tester. Bagian 4 dan 5 dipakai untuk
Pemesanan dan Pembayaran.

### Demo lokal tanpa Supabase (opsional)

Cara ini dipakai saat verifikasi implementasi dan tidak menyentuh Supabase:

```powershell
# 1. Siapkan classpath termasuk H2 dan resource test
.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt" "-Dmdep.includeScope=test"

# 2. Jalankan aplikasi dengan H2 in-memory + seed demo
$cp = "target\classes;target\test-classes;" + (Get-Content target\cp.txt -Raw).Trim()
java -cp $cp com.example.ubmager.UbmagerApplication --server.port=8099 `
  "--spring.datasource.url=jdbc:h2:mem:demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1" `
  --spring.datasource.username=sa --spring.datasource.password= `
  --spring.datasource.driver-class-name=org.h2.Driver `
  --spring.jpa.hibernate.ddl-auto=none --spring.jpa.show-sql=false `
  --spring.sql.init.mode=always `
  --spring.sql.init.schema-locations=classpath:schema-test.sql `
  --spring.sql.init.data-locations=classpath:demo-seed.sql
```

Seed membuat `USR001` (pembeli), `USR002` (penyedia), dan `PST001` (harga 50.000).
Alur lengkap sudah diuji dengan cara ini: buat pemesanan → terima → buat
pembayaran → bukti → konfirmasi → selesai.

## 10. Cara menguji

### Otomatis (tanpa Supabase)

```powershell
.\mvnw.cmd test
```

Test memakai H2 in-memory (`src/test/resources/application.properties`), bukan
database kelompok. Hasil terakhir: **37 test, 0 gagal** (36 test modul + 1 context).

### Manual dengan curl

```powershell
# Buat pemesanan
curl -X POST http://localhost:8080/api/pemesanan `
  -H "Content-Type: application/json" `
  -d '{\"id_pembeli\":\"USR001\",\"id_postingan\":\"PST001\",\"jumlah\":2}'

# Terima
curl -X PUT http://localhost:8080/api/pemesanan/PMS001/terima `
  -H "Content-Type: application/json" -d '{\"id_penyedia\":\"USR002\"}'

# Buat pembayaran
curl -X POST http://localhost:8080/api/pembayaran `
  -H "Content-Type: application/json" `
  -d '{\"id_pesanan\":\"PSN001\",\"metode\":\"transfer\"}'

# Bukti
curl -X POST http://localhost:8080/api/pembayaran/PBY001/bukti `
  -H "Content-Type: application/json" `
  -d '{\"bukti_pembayaran\":\"https://contoh.test/bukti.jpg\"}'

# Konfirmasi
curl -X PUT http://localhost:8080/api/pembayaran/PBY001/konfirmasi `
  -H "Content-Type: application/json" -d '{\"id_penyedia\":\"USR002\"}'

# Selesai
curl -X POST http://localhost:8080/api/pemesanan/PMS001/selesai `
  -H "Content-Type: application/json" -d '{\"id_penyedia\":\"USR002\"}'
```

## 11. Batasan yang masih ada

1. **Belum ada autentikasi/otorisasi Spring Security.** `id_pembeli` dan
   `id_penyedia` berasal dari request dan masih dapat dipalsukan. Pemeriksaan
   pemilik postingan hanya pemeriksaan logis, bukan keamanan penuh.
2. **Entity Postingan tidak tersedia.** Integrasi memakai native query baca-saja
   dengan asumsi kolom `id_postingan`, `id_penjual`, `harga`, `status_aktif`.
   Jika modul Postingan memakai nama/tipe berbeda, sesuaikan satu query tersebut
   dan FK di `02_pemesanan.sql`. Tabel `postingan` wajib sudah ada.
3. **`total_harga` data lama harus di-backfill manual.** Tidak ada nominal buatan.
4. **`IdGenerator` tidak aman untuk konkurensi.** Dua request bersamaan dapat
   memperoleh ID yang sama. Ini pola existing kelompok dan tidak diubah.
5. **Bukti pembayaran hanya URL/path.** Belum ada upload multipart/Supabase Storage.
6. **Belum ada pagination** pada daftar pemesanan. Volume data praktikum masih kecil.
7. **`Hasil` memetakan error berdasarkan substring pesan**, sehingga status HTTP
   terbatas 400/404. Otorisasi yang gagal dikembalikan sebagai 400, bukan 403.
8. **H2 bukan PostgreSQL penuh.** Test memverifikasi logika dan mapping, bukan
   seluruh perilaku constraint PostgreSQL. Verifikasi akhir tetap dilakukan pada
   project Supabase Anda.

---

# Materi Laporan Modul 3

## 3.2.8 Pembayaran

### Class utama

`PembayaranController`, `Pembayaran`, `DataPembayaran`,
`PembayaranRepository`, `DataPembayaranRepository`.

### Entity `Pembayaran`

```java
@Entity
@Table(name = "pembayaran")
public class Pembayaran {
    @Id
    @Column(name = "id_pembayaran", length = 10, nullable = false)
    private String idPembayaran;

    @Column(name = "id_pesanan", length = 10, nullable = false, unique = true)
    private String idPesanan;

    @Column(name = "metode", length = 50, nullable = false)
    private String metode;

    @Column(name = "jumlah", precision = 12, scale = 2, nullable = false)
    private BigDecimal jumlah;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "tanggal_bayar", nullable = false)
    private LocalDateTime tanggalBayar;

    // getter dan setter
}
```

### Entity `DataPembayaran`

```java
@Entity
@Table(name = "datapembayaran")
public class DataPembayaran {
    @Id
    @Column(name = "id_data_pembayaran", length = 10, nullable = false)
    private String idDataPembayaran;

    @Column(name = "id_pembayaran", length = 10, nullable = false)
    private String idPembayaran;

    @Column(name = "bukti_pembayaran", length = 255)
    private String buktiPembayaran;

    @Column(name = "tanggal_simpan", nullable = false)
    private LocalDateTime tanggalSimpan;

    // getter dan setter
}
```

### Membuat pembayaran

```java
private Hasil buatPembayaran(Map<String, Object> data) {
    String idPesanan = teks(data, "id_pesanan");
    String metode = teks(data, "metode").trim();

    if (idPesanan.isBlank()) {
        return new Hasil(false, "Data pembayaran tidak lengkap", null);
    }
    if (metode.isBlank()) {
        return new Hasil(false, "Metode pembayaran wajib diisi", null);
    }

    Optional<Pesanan> cariPesanan = pesananRepository.findById(idPesanan);
    if (cariPesanan.isEmpty()) {
        return new Hasil(false, "Pesanan tidak ditemukan", null);
    }

    Optional<Pemesanan> cariPemesanan = pemesananRepository.findByIdPesanan(idPesanan);
    if (cariPemesanan.isEmpty()) {
        return new Hasil(false, "Pemesanan tidak ditemukan", null);
    }
    if (!DITERIMA.equals(cariPemesanan.get().getStatusPesanan())) {
        return new Hasil(false, "Pemesanan belum diterima", null);
    }

    Pesanan pesanan = cariPesanan.get();
    if (!MENUNGGU_PESANAN.equals(pesanan.getStatusPesanan())) {
        return new Hasil(false, "Status pesanan tidak dapat dibayar", null);
    }
    if (pembayaranRepository.existsByIdPesanan(idPesanan)) {
        return new Hasil(false, "Pembayaran sudah ada untuk pesanan ini", null);
    }

    BigDecimal jumlah = pesanan.getTotalHarga();
    if (jumlah == null || jumlah.compareTo(BigDecimal.ZERO) <= 0) {
        return new Hasil(false, "Total harga pesanan tidak tersedia", null);
    }

    Pembayaran pembayaran = new Pembayaran();
    pembayaran.setIdPembayaran(idGenerator.buat("PBY", "pembayaran", "id_pembayaran"));
    pembayaran.setIdPesanan(idPesanan);
    pembayaran.setMetode(metode);
    pembayaran.setJumlah(jumlah);               // bukan dari request
    pembayaran.setStatus(MENUNGGU_KONFIRMASI);
    pembayaran.setTanggalBayar(LocalDateTime.now());
    pembayaranRepository.save(pembayaran);

    return new Hasil(true, "Pembayaran berhasil dibuat", dataPembayaran(pembayaran));
}
```

### Menyimpan bukti dan mengembalikan status setelah perbaikan

```java
DataPembayaran dataPembayaran = new DataPembayaran();
dataPembayaran.setIdDataPembayaran(
        idGenerator.buat("DPY", "datapembayaran", "id_data_pembayaran"));
dataPembayaran.setIdPembayaran(idPembayaran);
dataPembayaran.setBuktiPembayaran(bukti);
dataPembayaran.setTanggalSimpan(LocalDateTime.now());
dataPembayaranRepository.save(dataPembayaran);

if (PERBAIKAN.equals(pembayaran.getStatus())) {
    pembayaran.setStatus(MENUNGGU_KONFIRMASI);
    pembayaranRepository.save(pembayaran);
}
```

### Konfirmasi pembayaran

```java
pembayaran.setStatus(DIKONFIRMASI);
pembayaranRepository.save(pembayaran);

pesanan.setStatusPesanan(DIBAYAR);
pesananRepository.save(pesanan);
```

### Meminta perbaikan

```java
pembayaran.setStatus(PERBAIKAN);
pembayaranRepository.save(pembayaran);
// PESANAN.status_pesanan sengaja tidak diubah menjadi dibayar
```

### Pemeriksaan kewenangan penyedia

```java
private Hasil cekKewenanganPenyedia(String idPostingan, String idPenyedia) {
    if (idPenyedia.isBlank()) {
        return new Hasil(false, "ID penyedia wajib dikirim", null);
    }
    Optional<PostinganBacaRepository.Info> cariPostingan =
            postinganBacaRepository.cari(idPostingan);
    if (cariPostingan.isEmpty()) {
        return new Hasil(false, "Postingan tidak ditemukan", null);
    }
    if (!idPenyedia.equals(cariPostingan.get().idPenjual())) {
        return new Hasil(false, "Pengguna tidak berwenang atas pembayaran ini", null);
    }
    return null;
}
```

### Endpoint pembayaran

```java
@RestController
@RequestMapping("/api/pembayaran")
@Transactional
public class PembayaranController {

    @PostMapping
    public ResponseEntity<Hasil> buat(
            @RequestBody(required = false) Map<String, Object> body) {
        return buatPembayaran(data(body)).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/{idPembayaran}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idPembayaran) {
        return lihatPembayaran(idPembayaran).kirim(HttpStatus.OK);
    }

    @GetMapping("/pesanan/{idPesanan}")
    public ResponseEntity<Hasil> lihatBerdasarkanPesanan(@PathVariable String idPesanan) {
        // ...
    }

    @PostMapping("/{idPembayaran}/bukti")
    public ResponseEntity<Hasil> simpanBukti(
            @PathVariable String idPembayaran,
            @RequestBody(required = false) Map<String, Object> body) {
        return simpanBuktiPembayaran(idPembayaran, data(body)).kirim(HttpStatus.CREATED);
    }

    @PutMapping("/{idPembayaran}/konfirmasi")
    public ResponseEntity<Hasil> konfirmasi(
            @PathVariable String idPembayaran,
            @RequestBody(required = false) Map<String, Object> body) {
        return konfirmasiPembayaran(idPembayaran, data(body)).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPembayaran}/perbaikan")
    public ResponseEntity<Hasil> perbaikan(
            @PathVariable String idPembayaran,
            @RequestBody(required = false) Map<String, Object> body) {
        return mintaPerbaikan(idPembayaran, data(body)).kirim(HttpStatus.OK);
    }
}
```

## 3.2.9 Pemesanan

### Class utama

`PemesananController`, `Pemesanan`, `Pesanan`,
`PemesananRepository`, `PesananRepository`, `PostinganBacaRepository`.

### Entity `Pemesanan`

```java
@Entity
@Table(name = "pemesanan")
public class Pemesanan {
    @Id
    @Column(name = "id_pemesanan", length = 10, nullable = false)
    private String idPemesanan;

    @Column(name = "id_pesanan", length = 10, nullable = false, unique = true)
    private String idPesanan;

    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "id_pembeli", length = 10, nullable = false)
    private String idPembeli;

    @Column(name = "jumlah", nullable = false)
    private int jumlah;

    @Column(name = "waktu_pesanan")
    private LocalDateTime waktuPesanan;

    @Column(name = "lokasi", length = 255)
    private String lokasi;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "status_pesanan", length = 20, nullable = false)
    private String statusPesanan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // getter dan setter
}
```

### Tambahan pada `Pesanan`

```java
@Column(name = "tanggal_pesan")
private LocalDateTime tanggalPesan;

@Column(name = "total_harga", precision = 12, scale = 2)
private BigDecimal totalHarga;
```

### Membuat pesanan dan pemesanan dalam satu transaksi

```java
PostinganBacaRepository.Info postingan = cariPostingan.get();
if (!postingan.statusAktif()) {
    return new Hasil(false, "Postingan tidak aktif", null);
}

int jumlah = angka(teks(data, "jumlah"));
if (jumlah <= 0) {
    return new Hasil(false, "Jumlah harus lebih dari 0", null);
}

String teksWaktu = teks(data, "waktu_pesanan").trim();
LocalDateTime waktuPesanan = null;
if (!teksWaktu.isBlank()) {
    try {
        waktuPesanan = LocalDateTime.parse(teksWaktu.replace(' ', 'T'));
    } catch (DateTimeParseException e) {
        return new Hasil(false,
                "Format waktu_pesanan tidak valid. Contoh: 2026-12-01T10:00:00", null);
    }
}

BigDecimal totalHarga = postingan.harga()
        .multiply(BigDecimal.valueOf(jumlah))
        .setScale(2, RoundingMode.HALF_UP);
LocalDateTime sekarang = LocalDateTime.now();

Pesanan pesanan = new Pesanan();
pesanan.setIdPesanan(idGenerator.buat("PSN", "pesanan", "id_pesanan"));
pesanan.setIdPembeli(idPembeli);
pesanan.setIdPostingan(idPostingan);
pesanan.setStatusPesanan("menunggu");
pesanan.setTanggalPesan(sekarang);
pesanan.setTotalHarga(totalHarga);
pesananRepository.save(pesanan);

Pemesanan pemesanan = new Pemesanan();
pemesanan.setIdPemesanan(idGenerator.buat("PMS", "pemesanan", "id_pemesanan"));
pemesanan.setIdPesanan(pesanan.getIdPesanan());
pemesanan.setIdPostingan(idPostingan);
pemesanan.setIdPembeli(idPembeli);
pemesanan.setJumlah(jumlah);
pemesanan.setWaktuPesanan(waktuPesanan);
pemesanan.setLokasi(teksAtauNull(data, "lokasi"));
pemesanan.setCatatan(teksAtauNull(data, "catatan"));
pemesanan.setStatusPesanan("menunggu");
pemesanan.setCreatedAt(sekarang);
pemesananRepository.save(pemesanan);
```

Kedua `save` berada di method controller yang dianotasi `@Transactional` pada level
class, sehingga kegagalan salah satu penyimpanan membatalkan seluruh transaksi.

### Menerima pemesanan

```java
pemesanan.setStatusPesanan(DITERIMA);
pemesananRepository.save(pemesanan);
// PESANAN.status_pesanan tetap "menunggu"
```

### Menolak pemesanan

```java
pemesanan.setStatusPesanan(DITOLAK);
pemesananRepository.save(pemesanan);

pesanan.setStatusPesanan(DIBATALKAN);
pesananRepository.save(pesanan);
```

### Menyelesaikan layanan

```java
if (!DITERIMA.equals(pemesanan.getStatusPesanan())) {
    return new Hasil(false, "Pemesanan belum diterima", null);
}

Optional<Pembayaran> cariPembayaran =
        pembayaranRepository.findByIdPesanan(pemesanan.getIdPesanan());
if (cariPembayaran.isEmpty()) {
    return new Hasil(false, "Pembayaran tidak ditemukan", null);
}
if (!"dikonfirmasi".equals(cariPembayaran.get().getStatus())) {
    return new Hasil(false, "Pembayaran belum dikonfirmasi", null);
}

pesanan.setStatusPesanan("selesai");
pesananRepository.save(pesanan);
// PEMESANAN tetap "diterima" agar sesuai CHECK constraint
```

### Integrasi baca-saja Postingan

Entity Postingan belum tersedia. Karena itu dipakai satu repository native query,
tanpa entity baru dan tanpa operasi tulis:

```java
@Repository
public class PostinganBacaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Info> cari(String idPostingan) {
        List<?> hasil = entityManager.createNativeQuery(
                "SELECT id_postingan, id_penjual, harga, status_aktif FROM postingan"
                + " WHERE id_postingan = :idPostingan")
                .setParameter("idPostingan", idPostingan)
                .getResultList();
        // mapping ke record Info
    }

    public List<String> cariIdByPenjual(String idPenjual) {
        return entityManager.createNativeQuery(
                "SELECT id_postingan FROM postingan WHERE id_penjual = :idPenjual")
                .setParameter("idPenjual", idPenjual)
                .getResultList();
    }

    public record Info(String idPostingan, String idPenjual,
                       BigDecimal harga, boolean statusAktif) {
    }
}
```

### Endpoint pemesanan

```java
@RestController
@RequestMapping("/api/pemesanan")
@Transactional
public class PemesananController {

    @PostMapping
    public ResponseEntity<Hasil> buat(
            @RequestBody(required = false) Map<String, Object> body) {
        return buatPemesanan(data(body)).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/{idPemesanan}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idPemesanan) {
        return lihatPemesanan(idPemesanan).kirim(HttpStatus.OK);
    }

    @GetMapping("/pembeli/{idPembeli}")
    public ResponseEntity<Hasil> daftarPembeli(@PathVariable String idPembeli) {
        return daftarPemesananPembeli(idPembeli).kirim(HttpStatus.OK);
    }

    @GetMapping("/penyedia/{idPenyedia}")
    public ResponseEntity<Hasil> daftarPenyedia(@PathVariable String idPenyedia) {
        return daftarPemesananPenyedia(idPenyedia).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPemesanan}/terima")
    public ResponseEntity<Hasil> terima(@PathVariable String idPemesanan,
            @RequestBody(required = false) Map<String, Object> body) {
        return terimaPemesanan(idPemesanan, data(body)).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPemesanan}/tolak")
    public ResponseEntity<Hasil> tolak(@PathVariable String idPemesanan,
            @RequestBody(required = false) Map<String, Object> body) {
        return tolakPemesanan(idPemesanan, data(body)).kirim(HttpStatus.OK);
    }

    @PostMapping("/{idPemesanan}/selesai")
    public ResponseEntity<Hasil> selesai(@PathVariable String idPemesanan,
            @RequestBody(required = false) Map<String, Object> body) {
        return selesaikanPemesanan(idPemesanan, data(body)).kirim(HttpStatus.OK);
    }
}
```

## Ringkasan pengujian otomatis

| Kelas | Jumlah test | Cakupan |
|---|---|---|
| `PemesananControllerTest` | 16 | create valid, body kosong, pembeli/postingan tidak ada, postingan nonaktif, jumlah invalid, total harga, detail, daftar pembeli/penyedia, terima, tolak, transisi invalid, otorisasi penyedia, selesai, ID tidak ditemukan |
| `PembayaranControllerTest` | 13 | create valid, pesanan belum diterima, pembayaran ganda, nominal, bukti, kirim ulang setelah perbaikan, konfirmasi, perbaikan, penyedia salah, konfirmasi ganda, detail, cari by pesanan, ID tidak ditemukan |
| `UbmagerApplicationTests` | 1 | Context load |

Hasil: `Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`.
