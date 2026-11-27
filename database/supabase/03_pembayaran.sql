-- =====================================================================
-- 03. Tabel PEMBAYARAN
-- Prasyarat: tabel pesanan sudah ada dan terisi total_harga.
-- Satu pesanan hanya boleh memiliki satu pembayaran.
-- =====================================================================

CREATE TABLE IF NOT EXISTS pembayaran (
    id_pembayaran VARCHAR(10) PRIMARY KEY,
    id_pesanan    VARCHAR(10) NOT NULL,
    metode        VARCHAR(50) NOT NULL,
    jumlah        NUMERIC(12,2) NOT NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'menunggu_konfirmasi',
    tanggal_bayar TIMESTAMP NOT NULL,

    CONSTRAINT uq_pembayaran_pesanan UNIQUE (id_pesanan),
    CONSTRAINT ck_pembayaran_status
        CHECK (status IN ('menunggu_konfirmasi', 'perbaikan', 'dikonfirmasi')),
    CONSTRAINT ck_pembayaran_jumlah CHECK (jumlah > 0),
    CONSTRAINT fk_pembayaran_pesanan
        FOREIGN KEY (id_pesanan) REFERENCES pesanan (id_pesanan)
);
