package com.example.ubmager.controller;

import com.example.ubmager.controller.HalamanRiwayatController.KartuTransaksi;
import com.example.ubmager.entity.Transaksi;
import com.example.ubmager.repository.TransaksiRepository;
import com.example.ubmager.service.TransaksiService;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/riwayat")
public class HalamanRiwayatController {

    public record KartuTransaksi(String idTransaksi, String judul, String kategori, int jumlah,
            String total, String tanggal, String tanggalLengkap,
            String status, String statusLabel) {
    }

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter TAMPIL = DateTimeFormatter.ofPattern("dd MMM yyyy", ID);
    private static final DateTimeFormatter TAMPIL_LENGKAP = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", ID);
    private final RiwayatController riwayatController;
    private final TransaksiRepository transaksiRepository;
    private final TransaksiService transaksiService;

    public HalamanRiwayatController(RiwayatController riwayatController,
            TransaksiRepository transaksiRepository,
            TransaksiService transaksiService) {
        this.riwayatController = riwayatController;
        this.transaksiRepository = transaksiRepository;
        this.transaksiService = transaksiService;
    }

    // Halaman riwayat: tab peran, pencarian kata kunci, dan filter kategori
    @GetMapping
    public String lihat(@RequestParam(defaultValue = "pembeli") String peran,
            @RequestParam(defaultValue = "") String kategori,
            @RequestParam(defaultValue = "") String q,
            HttpSession session, Model model) {
        String idUser = idUser(session);
        if (idUser == null) {
            return "redirect:/login";
        }
        // Catat pesanan yang baru selesai (dari modul pemesanan dan pembayaran) ke tabel transaksi
        transaksiService.sinkronDariPesananSelesai();
        boolean penjual = peran.equalsIgnoreCase("penjual");
        kategori = kategori.trim();
        q = q.trim();

        List<Transaksi> semua = ambil(penjual, idUser, "", "");
        Set<String> daftarKategori = new TreeSet<>();
        for (Transaksi t : semua) {
            daftarKategori.add(t.getKategori());
        }
        List<Transaksi> hasil = (kategori.isEmpty() && q.isEmpty())
                ? semua
                : ambil(penjual, idUser, kategori, q);

        List<KartuTransaksi> kartu = hasil.stream().map(t -> new KartuTransaksi(
                t.getIdTransaksi(), t.getJudulPostingan(), t.getKategori(), t.getJumlah(),
                rupiah(t.getTotalHarga()), t.getTanggalTransaksi().format(TAMPIL),
                t.getTanggalTransaksi().format(TAMPIL_LENGKAP),
                t.getStatusTransaksi(), label(t.getStatusTransaksi()))).toList();

        model.addAttribute("peran", penjual ? "penjual" : "pembeli");
        model.addAttribute("kategori", kategori);
        model.addAttribute("q", q);
        model.addAttribute("daftarKategori", daftarKategori);
        model.addAttribute("kartu", kartu);
        model.addAttribute("adaTransaksi", !semua.isEmpty());
        return "riwayat/index";
    }

    // Unduh PDF memakai pengguna dari session, lalu memanfaatkan RiwayatController
    @GetMapping("/unduh/{idTransaksi}")
    public ResponseEntity<?> unduh(@PathVariable String idTransaksi, HttpSession session) {
        String idUser = idUser(session);
        if (idUser == null) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/login")).build();
        }
        return riwayatController.unduh(idTransaksi, idUser);
    }

    private List<Transaksi> ambil(boolean penjual, String idUser, String kategori, String kataKunci) {
        return penjual
                ? transaksiRepository.cariUntukPenjual(idUser, kategori, kataKunci)
                : transaksiRepository.cariUntukPembeli(idUser, kategori, kataKunci);
    }

    private String label(String status) {
        return switch (status) {
            case "selesai" -> "Selesai";
            case "dibayar" -> "Dibayar";
            case "menunggu" -> "Menunggu";
            case "dibatalkan" -> "Dibatalkan";
            default -> status;
        };
    }

    private String idUser(HttpSession session) {
        Object id = session.getAttribute("idUser");
        return id == null ? null : id.toString();
    }

    private String rupiah(double nilai) {
        NumberFormat format = NumberFormat.getInstance(ID);
        format.setMaximumFractionDigits(0);
        return "Rp " + format.format(nilai);
    }
}