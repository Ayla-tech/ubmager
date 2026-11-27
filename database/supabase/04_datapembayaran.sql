-- =====================================================================
-- 04. Tabel DATAPEMBAYARAN (riwayat bukti pembayaran)
-- Satu pembayaran boleh memiliki beberapa bukti (misalnya kirim ulang
-- setelah penyedia meminta perbaikan).
-- =====================================================================

CREATE TABLE IF NOT EXISTS datapembayaran (
    id_data_pembayaran VARCHAR(10) PRIMARY KEY,
    id_pembayaran      VARCHAR(10) NOT NULL,
    bukti_pembayaran   VARCHAR(255),
    tanggal_simpan     TIMESTAMP NOT NULL,

    CONSTRAINT fk_datapembayaran_pembayaran
        FOREIGN KEY (id_pembayaran) REFERENCES pembayaran (id_pembayaran)
);
