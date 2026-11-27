-- Skema H2 lengkap untuk verifikasi UI lokal (bukan untuk Supabase/kelompok).
-- Dipakai hanya oleh tools/ujicoba-ui.ps1.

DROP TABLE IF EXISTS dataotp;
DROP TABLE IF EXISTS datalaporan;
DROP TABLE IF EXISTS datareview;
DROP TABLE IF EXISTS datapembayaran;
DROP TABLE IF EXISTS pembayaran;
DROP TABLE IF EXISTS pemesanan;
DROP TABLE IF EXISTS pesanan;
DROP TABLE IF EXISTS postingan;
DROP TABLE IF EXISTS profil;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id_user VARCHAR(10) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email_atau_hp VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status_akun VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    update_at TIMESTAMP NOT NULL,
    rata_rata_rating REAL NOT NULL,
    badge_kepercayaan VARCHAR(30) NOT NULL
);

CREATE TABLE profil (
    id_profil VARCHAR(10) PRIMARY KEY,
    id_user VARCHAR(10) NOT NULL UNIQUE,
    nama_lengkap VARCHAR(100) NOT NULL,
    foto_profil VARCHAR(255),
    bio TEXT
);

CREATE TABLE postingan (
    id_postingan VARCHAR(10) PRIMARY KEY,
    id_penjual VARCHAR(10) NOT NULL,
    judul VARCHAR(100) NOT NULL,
    deskripsi TEXT NOT NULL,
    harga NUMERIC(12,2) NOT NULL,
    kategori VARCHAR(50) NOT NULL,
    jam_tayang TIMESTAMP NOT NULL,
    status_aktif BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE pesanan (
    id_pesanan VARCHAR(10) PRIMARY KEY,
    id_pembeli VARCHAR(10) NOT NULL,
    id_postingan VARCHAR(10) NOT NULL,
    status_pesanan VARCHAR(15) NOT NULL,
    tanggal_pesan TIMESTAMP,
    total_harga NUMERIC(12,2),
    CONSTRAINT ck_pesanan_status
        CHECK (status_pesanan IN ('menunggu', 'dibayar', 'selesai', 'dibatalkan'))
);

CREATE TABLE pemesanan (
    id_pemesanan VARCHAR(10) PRIMARY KEY,
    id_pesanan VARCHAR(10) NOT NULL UNIQUE,
    id_postingan VARCHAR(10) NOT NULL,
    id_pembeli VARCHAR(10) NOT NULL,
    jumlah INT NOT NULL,
    waktu_pesanan TIMESTAMP,
    lokasi VARCHAR(255),
    catatan TEXT,
    status_pesanan VARCHAR(20) NOT NULL DEFAULT 'menunggu',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_pemesanan_status
        CHECK (status_pesanan IN ('menunggu', 'diterima', 'ditolak')),
    CONSTRAINT ck_pemesanan_jumlah CHECK (jumlah > 0)
);

CREATE TABLE pembayaran (
    id_pembayaran VARCHAR(10) PRIMARY KEY,
    id_pesanan VARCHAR(10) NOT NULL UNIQUE,
    metode VARCHAR(50) NOT NULL,
    jumlah NUMERIC(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'menunggu_konfirmasi',
    tanggal_bayar TIMESTAMP NOT NULL,
    CONSTRAINT ck_pembayaran_status
        CHECK (status IN ('menunggu_konfirmasi', 'perbaikan', 'dikonfirmasi')),
    CONSTRAINT ck_pembayaran_jumlah CHECK (jumlah > 0)
);

CREATE TABLE datapembayaran (
    id_data_pembayaran VARCHAR(10) PRIMARY KEY,
    id_pembayaran VARCHAR(10) NOT NULL,
    bukti_pembayaran VARCHAR(255),
    tanggal_simpan TIMESTAMP NOT NULL
);

CREATE TABLE datareview (
    id_review VARCHAR(10) PRIMARY KEY,
    id_pesanan VARCHAR(10) NOT NULL,
    id_reviewer VARCHAR(10) NOT NULL,
    id_target VARCHAR(10) NOT NULL,
    rating SMALLINT NOT NULL,
    komentar TEXT,
    tanggal_review TIMESTAMP NOT NULL
);

CREATE TABLE datalaporan (
    id_laporan VARCHAR(10) PRIMARY KEY,
    id_review VARCHAR(10) NOT NULL,
    id_pelapor VARCHAR(10) NOT NULL,
    alasan_laporan TEXT NOT NULL,
    status_laporan VARCHAR(15) NOT NULL,
    tanggal_laporan TIMESTAMP NOT NULL
);

CREATE TABLE dataotp (
    id_otp VARCHAR(10) PRIMARY KEY,
    email_atau_hp VARCHAR(100) NOT NULL,
    kode_otp VARCHAR(6) NOT NULL,
    waktu_dibuat TIMESTAMP NOT NULL,
    waktu_kadaluarsa TIMESTAMP NOT NULL,
    jumlah_salah SMALLINT NOT NULL,
    status_otp VARCHAR(15) NOT NULL
);
