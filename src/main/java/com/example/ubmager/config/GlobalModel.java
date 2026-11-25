package com.example.ubmager.config;

import com.example.ubmager.dto.Sesi;
import com.example.ubmager.util.Teks;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Menyediakan data sesi (siapa yang login) ke semua template
@ControllerAdvice
public class GlobalModel {

    @ModelAttribute("sesi")
    public Sesi sesi(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new Sesi(false, null, null, null, null, false, null);
        }
        String idUser = (String) session.getAttribute("idUser");
        String namaAdmin = (String) session.getAttribute("namaAdmin");
        String nama = (String) session.getAttribute("nama");
        return new Sesi(idUser != null, idUser, (String) session.getAttribute("username"), nama,
                nama == null ? null : Teks.inisial(nama), session.getAttribute("idAdmin") != null, namaAdmin);
    }
}
