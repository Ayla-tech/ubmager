package com.example.ubmager.dto;

public record PostinganItem(String idPostingan, String judul, String deskripsi, String harga, String kategori,
                            String idPenjual, String namaPenjual, String inisial, String ratingTampil,
                            int bintang, boolean terpercaya, boolean adaReview) {
}
