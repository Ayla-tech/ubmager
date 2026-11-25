package com.example.ubmager.controller;

import com.example.ubmager.service.TampilanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BerandaController {

    private final TampilanService tampilanService;

    public BerandaController(TampilanService tampilanService) {
        this.tampilanService = tampilanService;
    }

    @GetMapping("/")
    public String beranda(Model model) {
        model.addAttribute("daftarPostingan", tampilanService.beranda());
        return "beranda";
    }

    // Halaman penguji API lama
    @GetMapping("/tester")
    public String tester() {
        return "tester";
    }
}
