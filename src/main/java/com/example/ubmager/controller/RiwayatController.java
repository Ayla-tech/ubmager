package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Transaksi;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.TransaksiRepository;
import com.example.ubmager.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/riwayat")
public class RiwayatController {

    private final UserRepository userRepository;
    private final TransaksiRepository transaksiRepository;
    private final PostinganRepository postinganRepository;

    public RiwayatController(UserRepository userRepository,
            TransaksiRepository transaksiRepository,
            PostinganRepository postinganRepository) {
        this.userRepository = userRepository;
        this.transaksiRepository = transaksiRepository;
        this.postinganRepository = postinganRepository;
    }

    // Lihat seluruh riwayat transaksi (peran: pembeli atau penjual)
    @GetMapping("/{idUser}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idUser,
            @RequestParam(defaultValue = "pembeli") String peran) {
        return prosesRiwayat("LIHAT_RIWAYAT", idUser,
                Map.of("peran", peran)).kirim(HttpStatus.OK);
    }

    // Filter riwayat berdasarkan kategori
    @GetMapping("/{idUser}/kategori")
    public ResponseEntity<Hasil> filter(@PathVariable String idUser,
            @RequestParam String kategori,
            @RequestParam(defaultValue = "pembeli") String peran) {
        return prosesRiwayat("FILTER_KATEGORI", idUser,
                Map.of("peran", peran, "kategori", kategori)).kirim(HttpStatus.OK);
    }

    // Cari riwayat berdasarkan kata kunci judul postingan
    @GetMapping("/{idUser}/cari")
    public ResponseEntity<Hasil> cari(@PathVariable String idUser,
            @RequestParam String kataKunci,
            @RequestParam(defaultValue = "pembeli") String peran) {
        return prosesRiwayat("CARI_KATA_KUNCI", idUser,
                Map.of("peran", peran, "kata_kunci", kataKunci)).kirim(HttpStatus.OK);
    }

    // Unduh bukti transaksi dalam bentuk PDF (maksimal 5 detik)
    @GetMapping("/unduh/{idTransaksi}")
    public ResponseEntity<?> unduh(@PathVariable String idTransaksi,
            @RequestParam String idUser) {
        Transaksi transaksi = getDetailTransaksi(idUser, idTransaksi);
        if (transaksi == null) {
            return new Hasil(false, "Transaksi tidak ditemukan", null).kirim(HttpStatus.NOT_FOUND);
        }
        try {
            byte[] pdf = CompletableFuture.supplyAsync(() -> generateBuktiPdf(transaksi))
                    .get(5, TimeUnit.SECONDS);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"bukti-" + idTransaksi + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return new Hasil(false, "Bukti transaksi gagal dibuat dalam batas waktu", null)
                    .kirim(HttpStatus.GATEWAY_TIMEOUT);
        }
    }

    // Method untuk memproses lihat, filter kategori, dan pencarian riwayat transaksi
    public Hasil prosesRiwayat(String aksi, String idUser, Map<String, Object> data) {
        if (!userRepository.existsById(idUser)) {
            return new Hasil(false, "Silakan login terlebih dahulu", null);
        }
        String peran = teks(data, "peran").equalsIgnoreCase("penjual") ? "penjual" : "pembeli";

        if (aksi.equals("LIHAT_RIWAYAT")) {
            List<Transaksi> daftar = ambilDaftar(peran, idUser, null, null);
            if (daftar.isEmpty()) {
                return new Hasil(true, "Belum ada riwayat transaksi", daftar);
            }
            return new Hasil(true, "Riwayat transaksi ditemukan", daftar);

        } else if (aksi.equals("FILTER_KATEGORI")) {
            String kategori = teks(data, "kategori");
            if (kategori.isBlank()) {
                return new Hasil(false, "Kategori tidak boleh kosong", null);
            }
            List<Transaksi> daftar = ambilDaftar(peran, idUser, kategori, null);
            if (daftar.isEmpty()) {
                return new Hasil(true, "Tidak ada riwayat transaksi pada kategori tersebut", daftar);
            }
            return new Hasil(true, "Riwayat transaksi sesuai kategori ditemukan", daftar);

        } else if (aksi.equals("CARI_KATA_KUNCI")) {
            String kataKunci = teks(data, "kata_kunci");
            if (kataKunci.isBlank()) {
                return new Hasil(false, "Kata kunci tidak boleh kosong", null);
            }
            List<Transaksi> daftar = ambilDaftar(peran, idUser, null, kataKunci);
            if (daftar.isEmpty()) {
                return new Hasil(true, "Tidak ada riwayat transaksi yang sesuai", daftar);
            }
            return new Hasil(true, "Riwayat transaksi sesuai kata kunci ditemukan", daftar);
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    // Method untuk mengambil daftar transaksi terbaru ke terlama sesuai peran pengguna
    private List<Transaksi> ambilDaftar(String peran, String idUser, String kategori, String kataKunci) {
        String k = kategori == null ? "" : kategori;
        String c = kataKunci == null ? "" : kataKunci;
        if (peran.equals("penjual")) {
            return transaksiRepository.cariUntukPenjual(idUser, k, c);
        }
        return transaksiRepository.cariUntukPembeli(idUser, k, c);
    }

    // Method untuk mengambil detail transaksi, hanya untuk pembeli atau penjual terkait
    public Transaksi getDetailTransaksi(String idUser, String idTransaksi) {
        Transaksi transaksi = transaksiRepository.findById(idTransaksi).orElse(null);
        if (transaksi == null) {
            return null;
        }
        boolean pembeli = transaksi.getIdUser().equals(idUser);
        boolean penjual = postinganRepository.findById(transaksi.getIdPostingan())
                .map(post -> post.getIdPenjual().equals(idUser))
                .orElse(false);
        return (pembeli || penjual) ? transaksi : null;
    }

    // Method untuk membuat dokumen PDF bukti transaksi
    public byte[] generateBuktiPdf(Transaksi transaksi) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        try (ByteArrayOutputStream keluaran = new ByteArrayOutputStream()) {
            Document dokumen = new Document();
            PdfWriter.getInstance(dokumen, keluaran);
            dokumen.open();
            dokumen.add(new Paragraph("BUKTI TRANSAKSI OFFER"));
            dokumen.add(new Paragraph(" "));
            dokumen.add(new Paragraph("ID Transaksi    : " + transaksi.getIdTransaksi()));
            dokumen.add(new Paragraph("Produk/Jasa     : " + transaksi.getJudulPostingan()));
            dokumen.add(new Paragraph("Kategori        : " + transaksi.getKategori()));
            dokumen.add(new Paragraph("Jumlah          : " + transaksi.getJumlah()));
            dokumen.add(new Paragraph("Total Harga     : " + String.format("Rp %,.2f", transaksi.getTotalHarga())));
            dokumen.add(new Paragraph("Tanggal         : " + transaksi.getTanggalTransaksi().format(format)));
            dokumen.add(new Paragraph("Status          : " + transaksi.getStatusTransaksi()));
            dokumen.close();
            return keluaran.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membuat PDF", e);
        }
    }

    private String teks(Map<String, Object> data, String kunci) {
        Object nilai = data.get(kunci);
        return nilai == null ? "" : nilai.toString();
    }
}