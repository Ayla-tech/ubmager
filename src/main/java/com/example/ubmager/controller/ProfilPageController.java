package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.dto.ProfilTampil;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.service.TampilanService;
import com.example.ubmager.util.Teks;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfilPageController {

    private final ProfilController profilController;
    private final TampilanService tampilanService;
    private final PesananRepository pesananRepository;
    private final PostinganRepository postinganRepository;

    public ProfilPageController(ProfilController profilController, TampilanService tampilanService,
                                PesananRepository pesananRepository, PostinganRepository postinganRepository) {
        this.profilController = profilController;
        this.tampilanService = tampilanService;
        this.pesananRepository = pesananRepository;
        this.postinganRepository = postinganRepository;
    }

    @GetMapping("/profil")
    public String saya(HttpSession session, Model model) {
        String idUser = (String) session.getAttribute("idUser");
        ProfilTampil profil = tampilanService.profil(idUser);
        if (profil == null) {
            session.invalidate();
            return "redirect:/login";
        }
        model.addAttribute("profil", profil);
        model.addAttribute("daftarReview", tampilanService.reviewUntuk(idUser));
        model.addAttribute("sudahDilapor", tampilanService.reviewSudahDilapor(idUser));
        return "profil/saya";
    }

    @PostMapping("/profil")
    public String ubah(@RequestParam("namaLengkap") String nama,
                       @RequestParam(value = "fotoProfil", defaultValue = "") String foto,
                       @RequestParam(value = "bio", defaultValue = "") String bio,
                       HttpSession session, RedirectAttributes ra) {
        String idUser = (String) session.getAttribute("idUser");
        nama = nama.trim();
        foto = foto.trim();
        bio = bio.trim();

        if (nama.isEmpty() || nama.length() > 100) {
            ra.addFlashAttribute("pesanError", "Nama lengkap wajib diisi, maksimal 100 karakter");
        } else if (!foto.isEmpty() && (foto.length() > 255 || !foto.matches("(?i)^https?://\\S+$"))) {
            ra.addFlashAttribute("pesanError", "Foto profil harus berupa tautan http atau https");
        } else if (bio.length() > 500) {
            ra.addFlashAttribute("pesanError", "Bio maksimal 500 karakter");
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("nama_lengkap", nama);
            data.put("foto_profil", foto);
            data.put("bio", bio);
            Hasil hasil = profilController.prosesKelolaProfil(idUser, "UPDATE", data, "", false);
            if (hasil.berhasil()) {
                session.setAttribute("nama", nama);
                ra.addFlashAttribute("pesanSukses", "Profil berhasil diperbarui");
            } else {
                ra.addFlashAttribute("pesanError", hasil.pesan());
            }
        }
        return "redirect:/profil";
    }

    @PostMapping("/profil/akun")
    public String kelolaAkun(@RequestParam("pilihan") String pilihan, @RequestParam("password") String password,
                             @RequestParam(value = "konfirmasi", required = false) String konfirmasi,
                             HttpSession session, RedirectAttributes ra) {
        String idUser = (String) session.getAttribute("idUser");

        if (pilihan.equals("HAPUS_PERMANEN")) {
            if (pesananRepository.existsByIdPembeli(idUser)) {
                ra.addFlashAttribute("pesanError",
                        "Akun dengan riwayat pesanan tidak bisa dihapus permanen. Pilih nonaktifkan sementara.");
                return "redirect:/profil";
            }
            if (postinganRepository.existsByIdPenjual(idUser)) {
                ra.addFlashAttribute("pesanError",
                        "Akun yang masih punya postingan tidak bisa dihapus permanen. Pilih nonaktifkan sementara.");
                return "redirect:/profil";
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pilihan_pengguna", pilihan);
        Hasil hasil = profilController.prosesKelolaProfil(idUser, "DELETE", data, password, konfirmasi != null);

        if (!hasil.berhasil()) {
            ra.addFlashAttribute("pesanError", "Gagal. Pastikan password benar dan kotak konfirmasi dicentang.");
            return "redirect:/profil";
        }
        session.invalidate();
        if (pilihan.equals("HAPUS_PERMANEN")) {
            ra.addFlashAttribute("pesanSukses", "Akunmu sudah dihapus permanen.");
            return "redirect:/";
        }
        ra.addFlashAttribute("pesanSukses", "Akunmu dinonaktifkan. Masuk lagi kapan saja untuk mengaktifkannya.");
        return "redirect:/login";
    }

    @GetMapping("/pengguna/{idUser}")
    public String publik(@PathVariable("idUser") String idUser, HttpSession session, Model model) {
        String saya = (String) session.getAttribute("idUser");
        if (idUser.equals(saya)) {
            return "redirect:/profil";
        }
        ProfilTampil profil = tampilanService.profil(idUser);
        if (profil == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("profil", profil);
        model.addAttribute("daftarReview", tampilanService.reviewUntuk(idUser));
        model.addAttribute("sudahDilapor", saya == null ? Set.<String>of() : tampilanService.reviewSudahDilapor(saya));
        return "profil/publik";
    }
}
