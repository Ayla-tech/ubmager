package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Conversation;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.repository.ConversationRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.service.ChatTampilanService;
import com.example.ubmager.util.Teks;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Halaman chat (Thymeleaf). Logika bisnis tetap berada di ChatController.
@Controller
public class ChatPageController {

    private final ChatController chatController;
    private final ChatTampilanService tampilan;
    private final ConversationRepository conversationRepository;
    private final PostinganRepository postinganRepository;

    public ChatPageController(ChatController chatController, ChatTampilanService tampilan,
            ConversationRepository conversationRepository, PostinganRepository postinganRepository) {
        this.chatController = chatController;
        this.tampilan = tampilan;
        this.conversationRepository = conversationRepository;
        this.postinganRepository = postinganRepository;
    }

    // Daftar seluruh percakapan
    @GetMapping("/chat")
    public String daftar(HttpSession session, Model model) {
        model.addAttribute("daftarPercakapan", tampilan.daftarPercakapan(idUser(session)));
        return "chat/daftar";
    }

    // Tombol "Chat penjual" pada postingan: buka atau buat ruang chat dengan pemilik postingan
    @GetMapping("/chat/mulai")
    public String mulai(@RequestParam("idPostingan") String idPostingan, HttpSession session,
            RedirectAttributes ra) {
        String idUser = idUser(session);
        Postingan post = postinganRepository.findById(idPostingan).orElse(null);
        if (post == null) {
            ra.addFlashAttribute("pesanError", "Postingan tidak ditemukan");
            return "redirect:/";
        }
        if (post.getIdPenjual().equals(idUser)) {
            ra.addFlashAttribute("pesanError", "Ini postinganmu sendiri");
            return "redirect:/";
        }

        Hasil hasil = chatController.prosesChat("BUKA_CHAT", Map.of(
                "id_user", idUser, "id_user_tujuan", post.getIdPenjual(), "id_post", idPostingan), null);
        if (!hasil.berhasil()) {
            ra.addFlashAttribute("pesanError", hasil.pesan());
            return "redirect:/";
        }
        Conversation conversation = (Conversation) ((Map<?, ?>) hasil.data()).get("conversation");
        return "redirect:/chat/" + conversation.getIdConversation();
    }

    // Ruang chat
    @GetMapping("/chat/{id}")
    public String ruang(@PathVariable("id") String id, HttpSession session, Model model, RedirectAttributes ra) {
        String idUser = idUser(session);
        Conversation conversation = percakapanMilik(id, idUser);
        if (conversation == null) {
            ra.addFlashAttribute("pesanError", "Percakapan tidak ditemukan");
            return "redirect:/chat";
        }

        String nama = tampilan.namaTampil(tampilan.idLawan(conversation, idUser));
        model.addAttribute("idConversation", id);
        model.addAttribute("namaLawan", nama);
        model.addAttribute("inisial", Teks.inisial(nama));
        model.addAttribute("judulPostingan", tampilan.judulPostingan(conversation.getIdPost()));
        model.addAttribute("pesan", tampilan.riwayat(id, idUser));
        return "chat/ruang";
    }

    // Potongan HTML daftar pesan, dipanggil berkala untuk memuat pesan baru
    @GetMapping("/chat/{id}/pesan")
    public String daftarPesan(@PathVariable("id") String id, HttpSession session, Model model) {
        String idUser = idUser(session);
        boolean milik = percakapanMilik(id, idUser) != null;
        model.addAttribute("pesan", milik ? tampilan.riwayat(id, idUser) : java.util.List.of());
        return "chat/ruang :: daftarPesan";
    }

    @PostMapping("/chat/{id}/kirim")
    public String kirim(@PathVariable("id") String id, @RequestParam(value = "teks", defaultValue = "") String teks,
            HttpSession session, RedirectAttributes ra) {
        Hasil hasil = chatController.prosesChat("KIRIM_PESAN", Map.of(
                "id_conversation", id, "id_pengirim", idUser(session), "teks", teks), null);
        if (!hasil.berhasil()) {
            ra.addFlashAttribute("pesanError", hasil.pesan());
        }
        return "redirect:/chat/" + id;
    }

    @PostMapping("/chat/{id}/gambar")
    public String kirimGambar(@PathVariable("id") String id, @RequestParam("file") MultipartFile file,
            HttpSession session, RedirectAttributes ra) {
        Hasil hasil = chatController.prosesChat("KIRIM_GAMBAR", Map.of(
                "id_conversation", id, "id_pengirim", idUser(session)), file);
        if (!hasil.berhasil()) {
            ra.addFlashAttribute("pesanError", hasil.pesan());
        }
        return "redirect:/chat/" + id;
    }

    @PostMapping("/chat/{id}/blokir")
    public String blokir(@PathVariable("id") String id, HttpSession session, RedirectAttributes ra) {
        String idUser = idUser(session);
        Conversation conversation = percakapanMilik(id, idUser);
        if (conversation == null) {
            ra.addFlashAttribute("pesanError", "Percakapan tidak ditemukan");
            return "redirect:/chat";
        }
        Hasil hasil = chatController.prosesChat("BLOKIR_LAPOR", Map.of("jenis_aksi", "BLOKIR",
                "id_user", idUser, "id_user_target", tampilan.idLawan(conversation, idUser)), null);
        ra.addFlashAttribute(hasil.berhasil() ? "pesanSukses" : "pesanError", hasil.pesan());
        return "redirect:/chat/" + id;
    }

    @PostMapping("/chat/{id}/lapor")
    public String lapor(@PathVariable("id") String id, @RequestParam(value = "alasan", defaultValue = "") String alasan,
            HttpSession session, RedirectAttributes ra) {
        String idUser = idUser(session);
        Conversation conversation = percakapanMilik(id, idUser);
        if (conversation == null) {
            ra.addFlashAttribute("pesanError", "Percakapan tidak ditemukan");
            return "redirect:/chat";
        }
        Hasil hasil = chatController.prosesChat("BLOKIR_LAPOR", Map.of("jenis_aksi", "LAPOR",
                "id_user", idUser, "id_user_target", tampilan.idLawan(conversation, idUser),
                "alasan", alasan), null);
        ra.addFlashAttribute(hasil.berhasil() ? "pesanSukses" : "pesanError", hasil.pesan());
        return "redirect:/chat/" + id;
    }

    // Percakapan hanya boleh dibuka oleh dua pihak yang terlibat
    private Conversation percakapanMilik(String id, String idUser) {
        return conversationRepository.findById(id)
                .filter(c -> idUser.equals(c.getIdUserPengirim()) || idUser.equals(c.getIdUserTujuan()))
                .orElse(null);
    }

    private String idUser(HttpSession session) {
        return (String) session.getAttribute("idUser");
    }
}
