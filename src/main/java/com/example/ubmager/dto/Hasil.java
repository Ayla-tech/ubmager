package com.example.ubmager.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record Hasil(boolean berhasil, String pesan, Object data) {
    public ResponseEntity<Hasil> kirim(HttpStatus kodeBerhasil) {
        HttpStatus kode = kodeBerhasil;
        if (!berhasil) {
            kode = pesan.contains("tidak ditemukan") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(kode).body(this);
    }
}
