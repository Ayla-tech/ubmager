package com.example.ubmager.dto;

public record Sesi(boolean login, String idUser, String username, String nama, String inisial,
                   boolean admin, String namaAdmin) {
}
