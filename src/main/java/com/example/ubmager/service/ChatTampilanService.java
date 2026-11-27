package com.example.ubmager.service;

import com.example.ubmager.dto.PercakapanItem;
import com.example.ubmager.dto.PesanItem;
import com.example.ubmager.entity.Conversation;
import com.example.ubmager.entity.Message;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.entity.Profil;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.ConversationRepository;
import com.example.ubmager.repository.MessageRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.ProfilRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.Teks;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

// Menyiapkan data chat untuk ditampilkan pada template
@Service
public class ChatTampilanService {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter WAKTU = DateTimeFormatter.ofPattern("d MMM, HH:mm", ID);
    private static final DateTimeFormatter JAM = DateTimeFormatter.ofPattern("HH:mm", ID);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProfilRepository profilRepository;
    private final UserRepository userRepository;
    private final PostinganRepository postinganRepository;

    public ChatTampilanService(ConversationRepository conversationRepository, MessageRepository messageRepository,
            ProfilRepository profilRepository, UserRepository userRepository,
            PostinganRepository postinganRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.profilRepository = profilRepository;
        this.userRepository = userRepository;
        this.postinganRepository = postinganRepository;
    }

    private record Baris(Conversation conversation, Message terakhir) {
    }

    // Daftar percakapan pengguna, diurutkan dari pesan terbaru
    public List<PercakapanItem> daftarPercakapan(String idUser) {
        List<Baris> baris = new ArrayList<>();
        for (Conversation c : conversationRepository.cariPercakapanUser(idUser)) {
            Message terakhir = messageRepository
                    .findFirstByIdConversationOrderByWaktuKirimDesc(c.getIdConversation()).orElse(null);
            baris.add(new Baris(c, terakhir));
        }
        baris.sort(Comparator.comparing(
                (Baris b) -> b.terakhir() == null ? LocalDateTime.MIN : b.terakhir().getWaktuKirim()).reversed());

        List<PercakapanItem> hasil = new ArrayList<>();
        for (Baris b : baris) {
            Conversation c = b.conversation();
            String nama = namaTampil(idLawan(c, idUser));
            hasil.add(new PercakapanItem(c.getIdConversation(), nama, Teks.inisial(nama),
                    judulPostingan(c.getIdPost()), ringkasan(b.terakhir()),
                    b.terakhir() == null ? "" : b.terakhir().getWaktuKirim().format(WAKTU)));
        }
        return hasil;
    }

    // Riwayat pesan dalam satu percakapan, urut kronologis
    public List<PesanItem> riwayat(String idConversation, String idUser) {
        List<PesanItem> hasil = new ArrayList<>();
        for (Message m : messageRepository.findByIdConversationOrderByWaktuKirimAsc(idConversation)) {
            hasil.add(new PesanItem(m.getIsiTeks(), m.getFileGambar(), m.getIdPengirim().equals(idUser),
                    m.getWaktuKirim().format(JAM)));
        }
        return hasil;
    }

    public String idLawan(Conversation conversation, String idUser) {
        return idUser.equals(conversation.getIdUserPengirim())
                ? conversation.getIdUserTujuan() : conversation.getIdUserPengirim();
    }

    public String namaTampil(String idUser) {
        return profilRepository.findByIdUser(idUser).map(Profil::getNamaLengkap)
                .orElseGet(() -> userRepository.findById(idUser).map(User::getUsername).orElse("Pengguna"));
    }

    public String judulPostingan(String idPost) {
        return postinganRepository.findById(idPost).map(Postingan::getJudul).orElse("Postingan");
    }

    private String ringkasan(Message pesan) {
        if (pesan == null) {
            return "Belum ada pesan";
        }
        if (pesan.getFileGambar() != null) {
            return "[Gambar]";
        }
        String isi = pesan.getIsiTeks() == null ? "" : pesan.getIsiTeks();
        return isi.length() > 60 ? isi.substring(0, 60) + "..." : isi;
    }
}
