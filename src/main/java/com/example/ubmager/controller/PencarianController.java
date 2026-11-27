package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.dto.PostinganItem;
import com.example.ubmager.service.TampilanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pencarian")
public class PencarianController {

    private final TampilanService tampilanService;

    public PencarianController(TampilanService tampilanService) {
        this.tampilanService = tampilanService;
    }

    @GetMapping("/cari")
    public ResponseEntity<Hasil> cari(
            @RequestParam(required = false) String kataKunci,
            @RequestParam(required = false) String kategori,
            @RequestParam(required = false) Double hargaMin,
            @RequestParam(required = false) Double hargaMax) {
        if (hargaMin != null && hargaMax != null && hargaMax < hargaMin) {
            return new Hasil(false, "Harga maksimum tidak boleh lebih kecil dari harga minimum", null).kirim(HttpStatus.BAD_REQUEST);
        }
        List<PostinganItem> hasil = tampilanService.cariPostingan(kataKunci, kategori, hargaMin, hargaMax);
        return new Hasil(true, "Pencarian berhasil", hasil).kirim(HttpStatus.OK);
    }
}