package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.dto.PesananItem;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.service.TampilanService;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewPageController {

    private final ReviewController reviewController;
    private final TampilanService tampilanService;
    private final PesananRepository pesananRepository;
    private final PostinganRepository postinganRepository;

    public ReviewPageController(ReviewController reviewController, TampilanService tampilanService,
                                PesananRepository pesananRepository, PostinganRepository postinganRepository) {
        this.reviewController = reviewController;
        this.tampilanService = tampilanService;
        this.pesananRepository = pesananRepository;
        this.postinganRepository = postinganRepository;
    }

    @GetMapping("/pesanan")
    public String pesananSaya(HttpSession session, Model model) {
        String idUser = (String) session.getAttribute("idUser");
        model.addAttribute("daftarPesanan", tampilanService.pesananSaya(idUser));
        return "review/pesanan";
    }

    @GetMapping("/review/baru")
    public String formReview(@RequestParam("idPesanan") String idPesanan, HttpSession session, Model model,
                             RedirectAttributes ra) {
        String idUser = (String) session.getAttribute("idUser");
        PesananItem pesanan = tampilanService.pesananUntukReview(idPesanan, idUser);
        if (pesanan == null) {
            ra.addFlashAttribute("pesanError", "Pesanan ini tidak bisa direview");
            return "redirect:/pesanan";
        }
        model.addAttribute("pesanan", pesanan);
        return "review/form";
    }

    @PostMapping("/review")
    public String simpan(@RequestParam("idPesanan") String idPesanan,
                         @RequestParam(value = "rating", defaultValue = "0") String rating,
                         @RequestParam(value = "komentar", defaultValue = "") String komentar,
                         HttpSession session, RedirectAttributes ra) {
        String idUser = (String) session.getAttribute("idUser");
        Pesanan pesanan = pesananRepository.findById(idPesanan).orElse(null);
        Postingan postingan = pesanan == null ? null : postinganRepository.findById(pesanan.getIdPostingan()).orElse(null);
        if (postingan == null) {
            ra.addFlashAttribute("pesanError", "Pesanan tidak ditemukan");
            return "redirect:/pesanan";
        }
        if (komentar.length() > 1000) {
            ra.addFlashAttribute("pesanError", "Komentar maksimal 1000 karakter");
            ra.addAttribute("idPesanan", idPesanan);
            return "redirect:/review/baru";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id_pesanan", idPesanan);
        data.put("id_reviewer", idUser);
        data.put("id_target", postingan.getIdPenjual());
        data.put("rating", rating);
        data.put("komentar", komentar.trim());

        Hasil hasil = reviewController.prosesReviewProfil("CREATE_REVIEW", data);
        if (hasil.berhasil()) {
            ra.addFlashAttribute("pesanSukses", "Terima kasih, review kamu sudah terkirim");
            return "redirect:/pesanan";
        }
        ra.addFlashAttribute("pesanError", hasil.pesan());
        ra.addAttribute("idPesanan", idPesanan);
        return "redirect:/review/baru";
    }

    @PostMapping("/review/laporan")
    public String lapor(@RequestParam("idReview") String idReview, @RequestParam("alasan") String alasan,
                        @RequestParam(value = "kembali", defaultValue = "/profil") String kembali,
                        HttpSession session, RedirectAttributes ra) {
        String idUser = (String) session.getAttribute("idUser");
        Map<String, Object> data = new HashMap<>();
        data.put("id_review", idReview);
        data.put("id_pelapor", idUser);
        data.put("alasan_laporan", alasan.trim());

        Hasil hasil = reviewController.prosesReviewProfil("REPORT_REVIEW", data);
        if (hasil.berhasil()) {
            ra.addFlashAttribute("pesanSukses", "Laporan terkirim. Admin akan meninjaunya.");
        } else {
            ra.addFlashAttribute("pesanError", hasil.pesan());
        }
        // Hanya izinkan alamat internal agar tidak jadi open redirect
        boolean aman = kembali.startsWith("/") && !kembali.startsWith("//");
        return "redirect:" + (aman ? kembali : "/profil");
    }
}
