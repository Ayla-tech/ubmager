package com.example.ubmager.dto;

// Data satu postingan jasa untuk halaman detail dan daftar jasa.
public record JasaTampil(String idPostingan, String judul, String deskripsi, String kategori,
                         String harga, long hargaAngka, String idPenjual, String namaPenjual,
                         String inisial, String ratingTampil, int bintang, boolean terpercaya,
                         boolean adaReview, boolean aktif) {
}
