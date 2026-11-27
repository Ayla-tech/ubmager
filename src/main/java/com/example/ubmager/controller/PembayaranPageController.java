package com.example.ubmager.controller;

import com.example.ubmager.dto.PembayaranTampil;
import com.example.ubmager.dto.PemesananTampil;
import com.example.ubmager.service.TransaksiTampilanService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

// Halaman pembayaran. Aksi tulis tetap melalui REST API PembayaranController.
@Controller
public class PembayaranPageController {

    private final TransaksiTampilanService transaksi;

    public PembayaranPageController(TransaksiTampilanService transaksi) {
        this.transaksi = transaksi;
    }

    @GetMapping("/pembayaran")
    public String daftarPembayaran(@RequestParam(defaultValue = "pembeli") String peran,
                                   HttpSession session, Model model) {
        String idUser = (String) session.getAttribute("idUser");
        boolean penyedia = "penyedia".equals(peran);

        model.addAttribute("peran", penyedia ? "penyedia" : "pembeli");
        if (penyedia) {
            List<PembayaranTampil> daftar = transaksi.pembayaranPenyedia(idUser);
            model.addAttribute("daftarPembayaran", daftar);
            model.addAttribute("jumlahMenunggu", daftar.stream()
                    .filter(p -> "menunggu_konfirmasi".equals(p.status())).count());
        } else {
            List<PemesananTampil> tagihan = transaksi.tagihanPembeli(idUser);
            List<PembayaranTampil> riwayat = transaksi.pembayaranPembeli(idUser);
            model.addAttribute("daftarTagihan", tagihan);
            model.addAttribute("daftarPembayaran", riwayat);
            model.addAttribute("jumlahMenunggu", riwayat.stream()
                    .filter(p -> "menunggu_konfirmasi".equals(p.status())
                            || "perbaikan".equals(p.status())).count());
        }
        return "pembayaran/index";
    }

    @GetMapping("/pembayaran/pesanan/{idPesanan}")
    public String formPembayaran(@PathVariable String idPesanan, HttpSession session, Model model) {
        PemesananTampil pemesanan = transaksi.pemesananBerdasarkanPesanan(idPesanan);
        if (pemesanan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String idUser = (String) session.getAttribute("idUser");
        if (idUser == null || !idUser.equals(pemesanan.idPembeli())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        PembayaranTampil pembayaran = transaksi.pembayaranBerdasarkanPesanan(idPesanan).orElse(null);
        if (pembayaran != null) {
            return "redirect:/pembayaran/" + pembayaran.idPembayaran();
        }
        if (!pemesanan.bisaBayar()) {
            return "redirect:/pemesanan/" + pemesanan.idPemesanan();
        }

        model.addAttribute("pemesanan", pemesanan);
        return "pembayaran/form";
    }

    @GetMapping("/pembayaran/{idPembayaran}")
    public String detailPembayaran(@PathVariable String idPembayaran, HttpSession session, Model model) {
        PembayaranTampil pembayaran = transaksi.pembayaran(idPembayaran);
        if (pembayaran == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String idUser = (String) session.getAttribute("idUser");
        boolean pembeli = idUser != null && idUser.equals(pembayaran.idPembeli());
        boolean penyedia = idUser != null && idUser.equals(pembayaran.idPenyedia());
        if (!pembeli && !penyedia) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("pembayaran", pembayaran);
        model.addAttribute("peranPenyedia", penyedia);
        return "pembayaran/detail";
    }
}
