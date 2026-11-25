package com.example.ubmager.dto;

public record ReviewItem(String idReview, String idReviewer, String usernameReviewer, String namaReviewer,
                         String inisial, int rating, String komentar, String tanggal) {
}
