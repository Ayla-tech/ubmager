package com.example.ubmager.config;

import com.example.ubmager.repository.NotifikasiRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Menyediakan jumlah notifikasi belum dibaca ke semua template (badge pada navbar)
@ControllerAdvice
public class NotifikasiModel {

    private final NotifikasiRepository notifikasiRepository;

    public NotifikasiModel(NotifikasiRepository notifikasiRepository) {
        this.notifikasiRepository = notifikasiRepository;
    }

    @ModelAttribute("belumDibaca")
    public long belumDibaca(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return 0;
        }
        String idUser = (String) session.getAttribute("idUser");
        return idUser == null ? 0 : notifikasiRepository.countByIdUserAndDibaca(idUser, false);
    }
}
