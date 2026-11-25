package com.example.ubmager.dto;

public record ProfilTampil(String idUser, String username, String kontak, String nama, String fotoUrl,
                           String bio, String ratingTampil, int bintang, String badge, String inisial,
                           int jumlahReview, String bergabung, boolean terpercaya) {
}
