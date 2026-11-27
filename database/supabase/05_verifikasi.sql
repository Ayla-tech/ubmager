-- =====================================================================
-- 05. Verifikasi skema (READ-ONLY, tidak mengubah data)
-- Jalankan setelah 01-04 berhasil.
-- =====================================================================

-- Struktur kolom
SELECT table_name, column_name, data_type, character_maximum_length, numeric_precision, numeric_scale
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('pesanan', 'pemesanan', 'pembayaran', 'datapembayaran')
ORDER BY table_name, ordinal_position;

-- Primary key, unique, check, dan foreign key
SELECT tc.table_name, tc.constraint_name, tc.constraint_type,
       kcu.column_name, ccu.table_name AS ref_table, ccu.column_name AS ref_column
FROM information_schema.table_constraints tc
LEFT JOIN information_schema.key_column_usage kcu
       ON tc.constraint_name = kcu.constraint_name
      AND tc.table_schema = kcu.table_schema
LEFT JOIN information_schema.constraint_column_usage ccu
       ON tc.constraint_name = ccu.constraint_name
      AND tc.table_schema = ccu.table_schema
WHERE tc.table_schema = 'public'
  AND tc.table_name IN ('pesanan', 'pemesanan', 'pembayaran', 'datapembayaran')
ORDER BY tc.table_name, tc.constraint_type, tc.constraint_name;

-- Cek data yang belum memiliki total_harga (harus di-backfill manual)
SELECT id_pesanan, id_pembeli, id_postingan, status_pesanan
FROM pesanan
WHERE total_harga IS NULL;

-- Cek data yang belum memiliki tanggal_pesan (harus di-backfill manual)
SELECT id_pesanan, id_pembeli, id_postingan, status_pesanan
FROM pesanan
WHERE tanggal_pesan IS NULL;

-- Default status pada tabel baru
SELECT table_name, column_name, column_default, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND ((table_name = 'pemesanan' AND column_name = 'status_pesanan')
    OR (table_name = 'pembayaran' AND column_name = 'status'))
ORDER BY table_name;

-- Cek duplikasi ID
SELECT id_pesanan FROM pesanan GROUP BY id_pesanan HAVING COUNT(*) > 1;
SELECT id_pemesanan FROM pemesanan GROUP BY id_pemesanan HAVING COUNT(*) > 1;
SELECT id_pembayaran FROM pembayaran GROUP BY id_pembayaran HAVING COUNT(*) > 1;

-- Cek panjang ID (maksimal 10 karakter)
SELECT id_pesanan FROM pesanan WHERE LENGTH(id_pesanan) > 10;
SELECT id_pemesanan FROM pemesanan WHERE LENGTH(id_pemesanan) > 10;
SELECT id_pembayaran FROM pembayaran WHERE LENGTH(id_pembayaran) > 10;
SELECT id_data_pembayaran FROM datapembayaran WHERE LENGTH(id_data_pembayaran) > 10;
