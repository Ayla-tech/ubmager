-- Data awal hanya untuk demo manual dengan H2 (bukan untuk Supabase/kelompok).
INSERT INTO users (id_user, username, email_atau_hp, password, status_akun,
                   created_at, update_at, rata_rata_rating, badge_kepercayaan)
VALUES ('USR001', 'pembeli_demo', 'pembeli_demo@contoh.test', 'demo',
        'aktif', NOW(), NOW(), 0, 'Pengguna Terpercaya Baru'),
       ('USR002', 'penyedia_demo', 'penyedia_demo@contoh.test', 'demo',
        'aktif', NOW(), NOW(), 0, 'Pengguna Terpercaya Baru');

INSERT INTO postingan (id_postingan, id_penjual, harga, status_aktif)
VALUES ('PST001', 'USR002', 50000.00, TRUE);
