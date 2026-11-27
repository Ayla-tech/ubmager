-- Data demo untuk verifikasi UI lokal. Bukan data Supabase/kelompok.
-- Akun demo: budi / sari / rina, password: demo12345

INSERT INTO users (id_user, username, email_atau_hp, password, status_akun,
                   created_at, update_at, rata_rata_rating, badge_kepercayaan)
VALUES ('USR001', 'budi', 'budi@contoh.test',
        '$2a$10$2YP9ET5ghqVa4/s7.PP56.CIGsHiGc7VhDKWp6n2SuIqw0ElK3h/2', 'aktif',
        '2026-01-10 09:00:00', '2026-11-20 09:00:00', 4.5, 'Pengguna Terpercaya Baru'),
       ('USR002', 'sari', 'sari@contoh.test',
        '$2a$10$2YP9ET5ghqVa4/s7.PP56.CIGsHiGc7VhDKWp6n2SuIqw0ElK3h/2', 'aktif',
        '2025-08-02 09:00:00', '2026-11-18 09:00:00', 4.8, 'Penjual Terpercaya'),
       ('USR003', 'rina', 'rina@contoh.test',
        '$2a$10$2YP9ET5ghqVa4/s7.PP56.CIGsHiGc7VhDKWp6n2SuIqw0ElK3h/2', 'aktif',
        '2026-03-14 09:00:00', '2026-11-10 09:00:00', 4.6, 'Penjual Terpercaya');

INSERT INTO profil (id_profil, id_user, nama_lengkap, foto_profil, bio)
VALUES ('PROF01', 'USR001', 'Budi Santoso', NULL, 'Suka jasa kreatif lokal.'),
       ('PROF02', 'USR002', 'Sari Craft', NULL, 'Kaligrafi dan desain visual untuk UMKM.'),
       ('PROF03', 'USR003', 'Rina Laundry', NULL, 'Laundry kiloan dan satuan.');

INSERT INTO postingan (id_postingan, id_penjual, judul, deskripsi, harga, kategori,
                       jam_tayang, status_aktif, created_at, updated_at)
VALUES ('PST001', 'USR002', 'Kaligrafi Custom Nama',
        'Kaligrafi tangan untuk nama, ayat, atau kutipan. Bisa request warna tinta dan ukuran kertas A3 atau A2.',
        75000.00, 'Kerajinan', '2026-11-20 08:00:00', TRUE,
        '2026-11-20 08:00:00', '2026-11-20 08:00:00'),
       ('PST002', 'USR002', 'Desain Logo UMKM',
        'Paket desain logo sederhana: dua alternatif konsep, revisi minor, dan file PNG transparan.',
        150000.00, 'Desain', '2026-11-21 08:00:00', TRUE,
        '2026-11-21 08:00:00', '2026-11-21 08:00:00'),
       ('PST003', 'USR003', 'Cuci Setrika 5 kg',
        'Paket cuci setrika kiloan untuk pakaian harian. Estimasi selesai dua hari kerja.',
        35000.00, 'Kebersihan', '2026-11-19 08:00:00', TRUE,
        '2026-11-19 08:00:00', '2026-11-19 08:00:00');

INSERT INTO pesanan (id_pesanan, id_pembeli, id_postingan, status_pesanan, tanggal_pesan, total_harga)
VALUES ('PSN001', 'USR001', 'PST001', 'menunggu', '2026-11-24 10:00:00', 150000.00),
       ('PSN002', 'USR001', 'PST001', 'menunggu', '2026-11-23 11:00:00', 75000.00),
       ('PSN003', 'USR001', 'PST002', 'dibatalkan', '2026-11-22 12:00:00', 150000.00),
       ('PSN004', 'USR001', 'PST002', 'dibayar', '2026-11-21 13:00:00', 450000.00),
       ('PSN005', 'USR001', 'PST001', 'dibayar', '2026-11-20 14:00:00', 75000.00),
       ('PSN006', 'USR001', 'PST002', 'menunggu', '2026-11-19 15:00:00', 150000.00),
       ('PSN007', 'USR001', 'PST002', 'selesai', '2026-11-10 16:00:00', 300000.00);

INSERT INTO pemesanan (id_pemesanan, id_pesanan, id_postingan, id_pembeli, jumlah,
                       waktu_pesanan, lokasi, catatan, status_pesanan, created_at)
VALUES ('PMS001', 'PSN001', 'PST001', 'USR001', 2,
        '2026-12-02 10:00:00', 'Jl. Merdeka 10, Bandung', 'Nama: Budi Santoso, tinta hitam.', 'menunggu', '2026-11-24 10:00:00'),
       ('PMS002', 'PSN002', 'PST001', 'USR001', 1,
        '2026-12-04 13:00:00', 'Kafe Kopi Pagi, Jl. Braga 5', NULL, 'diterima', '2026-11-23 11:00:00'),
       ('PMS003', 'PSN003', 'PST002', 'USR001', 1,
        '2026-11-30 09:00:00', 'Online', 'Butuh logo untuk toko kue.', 'ditolak', '2026-11-22 12:00:00'),
       ('PMS004', 'PSN004', 'PST002', 'USR001', 3,
        '2026-12-05 14:00:00', 'Online', 'Tiga varian logo untuk cabang.', 'diterima', '2026-11-21 13:00:00'),
       ('PMS005', 'PSN005', 'PST001', 'USR001', 1,
        '2026-11-28 10:00:00', 'Jl. Asia Afrika 8', 'Sudah selesai, terima kasih.', 'diterima', '2026-11-20 14:00:00'),
       ('PMS006', 'PSN006', 'PST002', 'USR001', 1,
        '2026-12-08 09:00:00', 'Online', 'Revisi konsep kedua.', 'diterima', '2026-11-19 15:00:00'),
       ('PMS007', 'PSN007', 'PST002', 'USR001', 2,
        '2026-11-15 10:00:00', 'Online', 'Logo dan kartu nama.', 'diterima', '2026-11-10 16:00:00');

INSERT INTO pembayaran (id_pembayaran, id_pesanan, metode, jumlah, status, tanggal_bayar)
VALUES ('PBY001', 'PSN004', 'Transfer Bank', 450000.00, 'menunggu_konfirmasi', '2026-11-24 15:30:00'),
       ('PBY002', 'PSN005', 'QRIS', 75000.00, 'dikonfirmasi', '2026-11-20 17:00:00'),
       ('PBY003', 'PSN006', 'E-Wallet', 150000.00, 'perbaikan', '2026-11-23 09:15:00'),
       ('PBY004', 'PSN007', 'Transfer Bank', 300000.00, 'dikonfirmasi', '2026-11-12 10:00:00');

INSERT INTO datapembayaran (id_data_pembayaran, id_pembayaran, bukti_pembayaran, tanggal_simpan)
VALUES ('DPY001', 'PBY001', 'https://example.com/bukti-transfer-pby001.jpg', '2026-11-24 15:35:00'),
       ('DPY002', 'PBY002', 'bukti/BUKTI-PBY002.jpg', '2026-11-20 17:05:00'),
       ('DPY003', 'PBY003', 'https://example.com/bukti-pby003-kurang-jelas.jpg', '2026-11-23 09:20:00'),
       ('DPY004', 'PBY004', 'bukti/BUKTI-PBY004.jpg', '2026-11-12 10:10:00');

INSERT INTO datareview (id_review, id_pesanan, id_reviewer, id_target, rating, komentar, tanggal_review)
VALUES ('REV001', 'PSN007', 'USR001', 'USR002', 5,
        'Komunikasi enak dan hasil desain rapi. Revisi cepat.', '2026-11-18 09:00:00');
