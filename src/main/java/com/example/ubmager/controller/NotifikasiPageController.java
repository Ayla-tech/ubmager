package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.dto.NotifikasiItem;
import com.example.ubmager.repository.NotifikasiRepository;
import jakarta.servlet.http.HttpSession;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Halaman notifikasi (Thymeleaf). Logika bisnis tetap berada di NotifikasiController.
@Controller
public class NotifikasiPageController {

    private static final DateTimeFormatter WAKTU =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));

    private final NotifikasiController notifikasiController;
    private final NotifikasiRepository notifikasiRepository;

    public NotifikasiPageController(NotifikasiController notifikasiController,
            NotifikasiRepository notifikasiRepository) {
        this.notifikasiController = notifikasiController;
        this.notifikasiRepository = notifikasiRepository;
    }

    // Daftar notifikasi, terbaru di atas
    @GetMapping("/notifikasi")
    public String daftar(HttpSession session, Model model) {
        List<NotifikasiItem> daftar = notifikasiRepository
                .findByIdUserOrderByCreatedAtDesc(idUser(session)).stream()
                .map(n -> new NotifikasiItem(n.getIdNotifikasi(), n.getTipe(), n.getPesan(),
                        n.getCreatedAt().format(WAKTU), n.isDibaca()))
                .toList();
        model.addAttribute("daftarNotifikasi", daftar);
        return "notifikasi/daftar";
    }

    // Buka notifikasi: tandai sudah dibaca lalu arahkan ke halaman terkait
    @PostMapping("/notifikasi/{id}/buka")
    public String buka(@PathVariable("id") String id, HttpSession session, RedirectAttributes ra) {
        Hasil hasil = notifikasiController.prosesNotifikasi("BUKA_NOTIFIKASI",
                Map.of("id_notifikasi", id, "id_user", idUser(session)));
        if (!hasil.berhasil()) {
            ra.addFlashAttribute("pesanError", hasil.pesan());
            return "redirect:/notifikasi";
        }
        return "redirect:" + ((Map<?, ?>) hasil.data()).get("url_tujuan");
    }

    // Tandai semua notifikasi sudah dibaca
    @PostMapping("/notifikasi/baca-semua")
    public String bacaSemua(HttpSession session, RedirectAttributes ra) {
        Hasil hasil = notifikasiController.prosesNotifikasi("TANDAI_SEMUA_DIBACA",
                Map.of("id_user", idUser(session)));
        ra.addFlashAttribute(hasil.berhasil() ? "pesanSukses" : "pesanError", hasil.pesan());
        return "redirect:/notifikasi";
    }

    private String idUser(HttpSession session) {
        return (String) session.getAttribute("idUser");
    }
}
