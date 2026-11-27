-- =====================================================================
-- 02. Tabel PEMESANAN (detail pemesanan jasa)
-- Prasyarat: tabel pesanan, postingan, dan users sudah ada.
-- =====================================================================

CREATE TABLE IF NOT EXISTS pemesanan (
    id_pemesanan   VARCHAR(10) PRIMARY KEY,
    id_pesanan     VARCHAR(10) NOT NULL,
    id_postingan   VARCHAR(10) NOT NULL,
    id_pembeli     VARCHAR(10) NOT NULL,
    jumlah         INT NOT NULL,
    waktu_pesanan  TIMESTAMP,
    lokasi         VARCHAR(255),
    catatan        TEXT,
    status_pesanan VARCHAR(20) NOT NULL DEFAULT 'menunggu',
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pemesanan_pesanan UNIQUE (id_pesanan),
    CONSTRAINT ck_pemesanan_status
        CHECK (status_pesanan IN ('menunggu', 'diterima', 'ditolak')),
    CONSTRAINT ck_pemesanan_jumlah CHECK (jumlah > 0),
    CONSTRAINT fk_pemesanan_pesanan
        FOREIGN KEY (id_pesanan) REFERENCES pesanan (id_pesanan),
    CONSTRAINT fk_pemesanan_postingan
        FOREIGN KEY (id_postingan) REFERENCES postingan (id_postingan),
    CONSTRAINT fk_pemesanan_pembeli
        FOREIGN KEY (id_pembeli) REFERENCES users (id_user)
);
