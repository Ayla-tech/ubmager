package com.example.ubmager.controller;

import com.example.ubmager.dto.JasaTampil;
import com.example.ubmager.dto.PemesananTampil;
import com.example.ubmager.service.TampilanService;
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

// Halaman jasa dan pemesanan. Aksi tulis tetap melalui REST API PemesananController.
@Controller
public class PemesananPageController {

    private final TransaksiTampilanService transaksi;
    private final TampilanService tampilanService;

    public PemesananPageController(TransaksiTampilanService transaksi, TampilanService tampilanService) {
        this.transaksi = transaksi;
        this.tampilanService = tampilanService;
    }

    @GetMapping("/jasa")
    public String daftarJasa(Model model) {
        model.addAttribute("daftarJasa", tampilanService.beranda());
        return "jasa/index";
    }

    @GetMapping("/jasa/{idPostingan}")
    public String detailJasa(@PathVariable String idPostingan, HttpSession session, Model model) {
        JasaTampil jasa = transaksi.jasa(idPostingan);
        if (jasa == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String idUser = (String) session.getAttribute("idUser");
        model.addAttribute("jasa", jasa);
        model.addAttribute("milikSendiri", idUser != null && idUser.equals(jasa.idPenjual()));
        return "jasa/detail";
    }

    @GetMapping("/pemesanan")
    public String daftarPemesanan(@RequestParam(defaultValue = "pembeli") String peran,
                                  HttpSession session, Model model) {
        String idUser = (String) session.getAttribute("idUser");
        boolean penyedia = "penyedia".equals(peran);
        List<PemesananTampil> daftar = penyedia
                ? transaksi.pemesananPenyedia(idUser)
                : transaksi.pemesananPembeli(idUser);

        model.addAttribute("peran", penyedia ? "penyedia" : "pembeli");
        model.addAttribute("daftarPemesanan", daftar);
        model.addAttribute("jumlahMenunggu", daftar.stream()
                .filter(p -> "menunggu".equals(p.status())).count());
        model.addAttribute("jumlahAktif", daftar.stream()
                .filter(p -> "diterima".equals(p.status()) && !"selesai".equals(p.statusPesanan())).count());
        return "pemesanan/index";
    }

    @GetMapping("/pemesanan/{idPemesanan}")
    public String detailPemesanan(@PathVariable String idPemesanan, HttpSession session, Model model) {
        PemesananTampil pemesanan = transaksi.pemesanan(idPemesanan);
        if (pemesanan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String idUser = (String) session.getAttribute("idUser");
        boolean pembeli = idUser != null && idUser.equals(pemesanan.idPembeli());
        boolean penyedia = idUser != null && idUser.equals(pemesanan.idPenyedia());
        if (!pembeli && !penyedia) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("pemesanan", pemesanan);
        model.addAttribute("peranPenyedia", penyedia);
        return "pemesanan/detail";
    }
}
