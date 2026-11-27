package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.DataOTPRepository;
import com.example.ubmager.repository.ProfilRepository;
import com.example.ubmager.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final String KUNCI_REGISTRASI = "dataRegistrasi";

    private final ProfilController profilController;
    private final UserRepository userRepository;
    private final ProfilRepository profilRepository;
    private final DataOTPRepository dataOTPRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${ubmager.demo.tampilkan-otp:false}")
    private boolean tampilkanOtp;

    public AuthController(ProfilController profilController, UserRepository userRepository,
                          ProfilRepository profilRepository, DataOTPRepository dataOTPRepository) {
        this.profilController = profilController;
        this.userRepository = userRepository;
        this.profilRepository = profilRepository;
        this.dataOTPRepository = dataOTPRepository;
    }

    // ---------- Login ----------
    @GetMapping("/login")
    public String formLogin(HttpSession session) {
        if (session.getAttribute("idUser") != null) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("identitas") String identitas, @RequestParam("password") String password,
                        HttpServletRequest request, Model model, RedirectAttributes ra) {
        String kunci = identitas.trim();
        Optional<User> cari = userRepository.findByUsernameOrEmailAtauHp(kunci, kunci);

        if (cari.isEmpty() || cari.get().getStatusAkun().equals("dihapus")
                || !encoder.matches(password, cari.get().getPassword())) {
            model.addAttribute("pesanError", "Username, email, atau password salah");
            model.addAttribute("identitas", identitas);
            return "auth/login";
        }

        User user = cari.get();
        boolean diaktifkan = false;
        if (user.getStatusAkun().equals("nonaktif_sementara")) {
            user.setStatusAkun("aktif");
            user.setUpdateAt(LocalDateTime.now());
            userRepository.save(user);
            diaktifkan = true;
        }

        String nama = profilRepository.findByIdUser(user.getIdUser())
                .map(p -> p.getNamaLengkap()).orElse(user.getUsername());

        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute("idUser", user.getIdUser());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("nama", nama);

        if (diaktifkan) {
            ra.addFlashAttribute("pesanSukses", "Selamat datang kembali. Akunmu sudah diaktifkan lagi.");
        }
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ---------- Daftar ----------
    @GetMapping("/daftar")
    public String formDaftar(HttpSession session) {
        if (session.getAttribute("idUser") != null) {
            return "redirect:/";
        }
        return "auth/daftar";
    }

    @PostMapping("/daftar")
    public String daftar(@RequestParam("username") String username, @RequestParam("emailAtauHp") String emailAtauHp,
                         @RequestParam("namaLengkap") String namaLengkap, @RequestParam("password") String password,
                         HttpSession session, Model model) {
        username = username.trim();
        emailAtauHp = emailAtauHp.trim();
        namaLengkap = namaLengkap.trim();

        String error = null;
        if (!username.matches("[A-Za-z0-9_.]{3,50}")) {
            error = "Username 3 sampai 50 karakter, hanya huruf, angka, titik, dan garis bawah";
        } else if (emailAtauHp.isEmpty() || emailAtauHp.length() > 100) {
            error = "Email atau nomor HP wajib diisi";
        } else if (namaLengkap.isEmpty() || namaLengkap.length() > 100) {
            error = "Nama lengkap wajib diisi";
        } else if (password.length() < 8) {
            error = "Password minimal 8 karakter";
        } else if (userRepository.existsByUsername(username)) {
            error = "Username sudah dipakai";
        } else if (userRepository.existsByEmailAtauHp(emailAtauHp)) {
            error = "Email atau nomor HP sudah terdaftar";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email_atau_hp", emailAtauHp);
        data.put("password", password);
        data.put("nama_lengkap", namaLengkap);

        if (error == null) {
            Hasil hasil = profilController.prosesKelolaProfil(null, "CREATE", data, "", false);
            if (!hasil.berhasil()) {
                error = hasil.pesan();
            }
        }
        if (error != null) {
            model.addAttribute("pesanError", error);
            model.addAttribute("username", username);
            model.addAttribute("emailAtauHp", emailAtauHp);
            model.addAttribute("namaLengkap", namaLengkap);
            return "auth/daftar";
        }

        session.setAttribute(KUNCI_REGISTRASI, data);
        return "redirect:/daftar/verifikasi";
    }

    // ---------- Verifikasi OTP ----------
    @GetMapping("/daftar/verifikasi")
    public String formOtp(HttpSession session, Model model) {
        Map<String, Object> data = dataRegistrasi(session);
        if (data == null) {
            return "redirect:/daftar";
        }
        isiDataOtp(data, model);
        return "auth/otp";
    }

    @PostMapping("/daftar/verifikasi")
    public String verifikasi(@RequestParam("kode") String kode, HttpSession session, Model model,
                             RedirectAttributes ra) {
        Map<String, Object> data = dataRegistrasi(session);
        if (data == null) {
            return "redirect:/daftar";
        }
        Map<String, Object> isi = new HashMap<>(data);
        isi.put("kode_input", kode.trim());

        Hasil hasil = profilController.prosesKelolaProfil(null, "VERIFIKASI_OTP", isi, "", false);
        if (!hasil.berhasil()) {
            model.addAttribute("pesanError", "Kode OTP salah atau sudah kedaluwarsa");
            isiDataOtp(data, model);
            return "auth/otp";
        }

        session.removeAttribute(KUNCI_REGISTRASI);
        ra.addFlashAttribute("pesanSukses", "Akun berhasil dibuat. Silakan masuk.");
        return "redirect:/login";
    }

    @PostMapping("/daftar/kirim-ulang")
    public String kirimUlang(HttpSession session, RedirectAttributes ra) {
        Map<String, Object> data = dataRegistrasi(session);
        if (data == null) {
            return "redirect:/daftar";
        }
        Hasil hasil = profilController.prosesKelolaProfil(null, "CREATE", data, "", false);
        if (hasil.berhasil()) {
            ra.addFlashAttribute("pesanSukses", "Kode OTP baru sudah dikirim");
        } else {
            ra.addFlashAttribute("pesanError", hasil.pesan());
        }
        return "redirect:/daftar/verifikasi";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataRegistrasi(HttpSession session) {
        return (Map<String, Object>) session.getAttribute(KUNCI_REGISTRASI);
    }

    private void isiDataOtp(Map<String, Object> data, Model model) {
        String kontak = String.valueOf(data.get("email_atau_hp"));
        model.addAttribute("kontak", kontak);
        if (tampilkanOtp) {
            dataOTPRepository.findFirstByEmailAtauHpOrderByWaktuDibuatDesc(kontak)
                    .ifPresent(otp -> model.addAttribute("kodeDemo", otp.getKodeOtp()));
        }
    }
}
