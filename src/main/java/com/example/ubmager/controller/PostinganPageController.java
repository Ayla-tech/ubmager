package com.example.ubmager.controller;

import com.example.ubmager.service.TampilanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PostinganPageController {

    private final TampilanService tampilanService;

    public PostinganPageController(TampilanService tampilanService) {
        this.tampilanService = tampilanService;
    }

    @GetMapping("/kelola-postingan")
    public String kelolaPostingan() {
        return "kelola-postingan";
    }

    @GetMapping("/pencarian")
    public String pencarian(Model model) {
        model.addAttribute("daftarPostingan", tampilanService.beranda());
        return "beranda";
    }
}
