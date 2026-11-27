-- Menambahkan tanggal pemesanan pada tabel PESANAN.
-- Dibuat nullable agar data transaksi lama tidak terganggu.

ALTER TABLE public.pesanan
ADD COLUMN IF NOT EXISTS tanggal_pesan TIMESTAMP;

-- Menambahkan total harga transaksi.
-- Dibuat nullable karena nominal transaksi lama belum tersedia.

ALTER TABLE public.pesanan
ADD COLUMN IF NOT EXISTS total_harga NUMERIC(12,2);