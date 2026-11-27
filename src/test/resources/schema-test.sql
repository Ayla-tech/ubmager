-- Skema minimum untuk pengujian modul Pemesanan dan Pembayaran.
-- Ditulis mengikuti DDL Supabase (PostgreSQL), dijalankan pada H2 mode PostgreSQL.

DROP TABLE IF EXISTS datapembayaran;
DROP TABLE IF EXISTS pembayaran;
DROP TABLE IF EXISTS pemesanan;
DROP TABLE IF EXISTS pesanan;
DROP TABLE IF EXISTS postingan;
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

CREATE TABLE postingan (
    id_postingan VARCHAR(10) PRIMARY KEY,
    id_penjual VARCHAR(10) NOT NULL,
    harga NUMERIC(12,2) NOT NULL,
    status_aktif BOOLEAN NOT NULL
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
    CONSTRAINT ck_pemesanan_jumlah CHECK (jumlah > 0),
    CONSTRAINT fk_pemesanan_pesanan FOREIGN KEY (id_pesanan) REFERENCES pesanan (id_pesanan),
    CONSTRAINT fk_pemesanan_postingan FOREIGN KEY (id_postingan) REFERENCES postingan (id_postingan),
    CONSTRAINT fk_pemesanan_pembeli FOREIGN KEY (id_pembeli) REFERENCES users (id_user)
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
    CONSTRAINT ck_pembayaran_jumlah CHECK (jumlah > 0),
    CONSTRAINT fk_pembayaran_pesanan FOREIGN KEY (id_pesanan) REFERENCES pesanan (id_pesanan)
);

CREATE TABLE datapembayaran (
    id_data_pembayaran VARCHAR(10) PRIMARY KEY,
    id_pembayaran VARCHAR(10) NOT NULL,
    bukti_pembayaran VARCHAR(255),
    tanggal_simpan TIMESTAMP NOT NULL,
    CONSTRAINT fk_datapembayaran_pembayaran
        FOREIGN KEY (id_pembayaran) REFERENCES pembayaran (id_pembayaran)
);
