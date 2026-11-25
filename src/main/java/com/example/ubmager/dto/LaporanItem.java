package com.example.ubmager.dto;

public record LaporanItem(String idLaporan, String status, String alasan, String tanggal, String pelapor,
                          String idReview, int rating, String komentar, String reviewer, String target,
                          String tanggalReview, boolean selesai) {
}
