package com.example.ubmager.dto;

import java.util.List;

// Data pembayaran yang sudah digabung dengan pesanan, pemesanan, dan postingan.
public record PembayaranTampil(String idPembayaran, String idPesanan, String idPemesanan,
                               String idPostingan, String judul, String total, String metode,
                               String status, String statusLabel, String statusKelas,
                               String tanggalBayar,
                               String idPembeli, String namaPembeli, String inisialPembeli,
                               String idPenyedia, String namaPenyedia, String inisialPenyedia,
                               String statusPesanan, String statusPesananLabel, String statusPesananKelas,
                               String buktiTerakhir, String tanggalBuktiTerakhir, boolean adaBukti,
                               boolean bisaKonfirmasi, boolean bisaPerbaikan,
                               boolean bisaKirimBukti, boolean bisaSelesai,
                               List<BuktiTampil> daftarBukti) {
}
