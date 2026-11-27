package com.example.ubmager.dto;

public record PesananItem(String idPesanan, String judul, String harga, String status, String idPenjual,
                          String namaPenjual, boolean sudahReview) {
}
