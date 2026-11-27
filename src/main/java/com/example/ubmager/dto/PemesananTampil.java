package com.example.ubmager.dto;

// Data pemesanan yang sudah digabung dengan pesanan, postingan, dan pembayaran.
public record PemesananTampil(String idPemesanan, String idPesanan, String idPostingan,
                              String judul, String kategori, String hargaSatuan, int jumlah,
                              String total, String waktu, String lokasi, String catatan, String dibuat,
                              String status, String statusLabel, String statusKelas,
                              String statusPesanan, String statusPesananLabel, String statusPesananKelas,
                              String idPembeli, String namaPembeli, String inisialPembeli,
                              String idPenyedia, String namaPenyedia, String inisialPenyedia,
                              String idPembayaran, String statusPembayaran,
                              String statusPembayaranLabel, String statusPembayaranKelas,
                              boolean bisaBayar, boolean sudahBayar, boolean bisaSelesai) {
}
