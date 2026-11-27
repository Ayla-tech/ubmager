package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.service.PostinganService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/postingan")
public class PostinganController {

    private final PostinganService postinganService;

    public PostinganController(PostinganService postinganService) {
        this.postinganService = postinganService;
    }

    @PostMapping("/buat")
    public ResponseEntity<Hasil> buat(@RequestBody Map<String, Object> body) {
        return postinganService.prosesKelolaPostingan("CREATE", body).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/penjual/{idPenjual}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idPenjual) {
        return postinganService.prosesKelolaPostingan("READ", Map.of("id_penjual", idPenjual)).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPostingan}")
    public ResponseEntity<Hasil> ubah(@PathVariable String idPostingan, @RequestBody Map<String, Object> body) {
        body.put("id_postingan", idPostingan);
        return postinganService.prosesKelolaPostingan("UPDATE", body).kirim(HttpStatus.OK);
    }

    @DeleteMapping("/{idPostingan}")
    public ResponseEntity<Hasil> hapus(@PathVariable String idPostingan, @RequestBody Map<String, Object> body) {
        body.put("id_postingan", idPostingan);
        return postinganService.prosesKelolaPostingan("DELETE", body).kirim(HttpStatus.OK);
    }
}