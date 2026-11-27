package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Admin;
import com.example.ubmager.repository.AdminRepository;
import com.example.ubmager.service.AdminService;
import com.example.ubmager.service.TampilanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminRepository adminRepository;
    private final AdminService adminService;
    private final TampilanService tampilanService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminController(AdminRepository adminRepository, AdminService adminService,
                           TampilanService tampilanService) {
        this.adminRepository = adminRepository;
        this.adminService = adminService;
        this.tampilanService = tampilanService;
    }

    @GetMapping("/login")
    public String formLogin(HttpSession session) {
        if (session.getAttribute("idAdmin") != null) {
            return "redirect:/admin";
        }
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username, @RequestParam("password") String password,
                        HttpServletRequest request, Model model) {
        Optional<Admin> cari = adminRepository.findByUsername(username.trim());
        if (cari.isEmpty() || !encoder.matches(password, cari.get().getPassword())) {
            model.addAttribute("pesanError", "Kredensial admin salah");
            model.addAttribute("username", username);
            return "admin/login";
        }
        HttpSession session = request.getSession();
        request.changeSessionId();
        session.setAttribute("idAdmin", cari.get().getIdAdmin());
        session.setAttribute("namaAdmin", cari.get().getNamaAdmin());
        return "redirect:/admin";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("idAdmin");
        session.removeAttribute("namaAdmin");
        return "redirect:/admin/login";
    }

    @GetMapping({"", "/"})
    public String dashboard(@RequestParam(value = "status", defaultValue = "semua") String status, Model model) {
        model.addAttribute("filter", status);
        model.addAttribute("daftarLaporan", tampilanService.laporan(status));
        model.addAttribute("hitung", tampilanService.hitungLaporan());
        return "admin/dashboard";
    }

    @PostMapping("/laporan/{idLaporan}/status")
    public String ubahStatus(@PathVariable("idLaporan") String idLaporan, @RequestParam("status") String status,
                             @RequestParam(value = "filter", defaultValue = "semua") String filter,
                             RedirectAttributes ra) {
        Hasil hasil = adminService.ubahStatus(idLaporan, status);
        ra.addFlashAttribute(hasil.berhasil() ? "pesanSukses" : "pesanError", hasil.pesan());
        ra.addAttribute("status", filter);
        return "redirect:/admin";
    }
}
