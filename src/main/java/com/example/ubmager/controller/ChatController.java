package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Conversation;
import com.example.ubmager.entity.Message;
import com.example.ubmager.entity.User;
import com.example.ubmager.entity.UserBlock;
import com.example.ubmager.entity.UserReport;
import com.example.ubmager.repository.ConversationRepository;
import com.example.ubmager.repository.MessageRepository;
import com.example.ubmager.repository.UserBlockRepository;
import com.example.ubmager.repository.UserReportRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
@Transactional
public class ChatController {

    private static final long UKURAN_MAKS = 5L * 1024 * 1024;
    private static final Set<String> EKSTENSI_BOLEH = Set.of("jpg", "jpeg", "png");
    private static final String FOLDER_UPLOAD = "uploads/chat";

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserReportRepository userReportRepository;
    private final NotifikasiController notifikasiController;
    private final IdGenerator idGenerator;

    public ChatController(UserRepository userRepository, ConversationRepository conversationRepository,
            MessageRepository messageRepository, UserBlockRepository userBlockRepository,
            UserReportRepository userReportRepository, NotifikasiController notifikasiController,
            IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userBlockRepository = userBlockRepository;
        this.userReportRepository = userReportRepository;
        this.notifikasiController = notifikasiController;
        this.idGenerator = idGenerator;
    }

    // Buka ruang chat dan riwayat pesan (id pengguna diambil dari session)
    @PostMapping("/buka")
    public ResponseEntity<Hasil> buka(@RequestBody Map<String, Object> body, HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        Map<String, Object> data = new HashMap<>(body);
        data.put("id_user", idUser);
        return prosesChat("BUKA_CHAT", data, null).kirim(HttpStatus.OK);
    }

    // Kirim pesan teks
    @PostMapping("/pesan")
    public ResponseEntity<Hasil> kirimPesan(@RequestBody Map<String, Object> body, HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        Map<String, Object> data = new HashMap<>(body);
        data.put("id_pengirim", idUser);
        return prosesChat("KIRIM_PESAN", data, null).kirim(HttpStatus.CREATED);
    }

    // Kirim pesan gambar
    @PostMapping(value = "/gambar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Hasil> kirimGambar(@RequestParam("id_conversation") String idConversation,
            @RequestParam("file") MultipartFile file, HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        Map<String, Object> data = Map.of("id_conversation", idConversation, "id_pengirim", idUser);
        return prosesChat("KIRIM_GAMBAR", data, file).kirim(HttpStatus.CREATED);
    }

    // Blokir pengguna
    @PostMapping("/blokir")
    public ResponseEntity<Hasil> blokir(@RequestBody Map<String, Object> body, HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        Map<String, Object> data = new HashMap<>(body);
        data.put("jenis_aksi", "BLOKIR");
        data.put("id_user", idUser);
        return prosesChat("BLOKIR_LAPOR", data, null).kirim(HttpStatus.OK);
    }

    // Laporkan pengguna
    @PostMapping("/laporan")
    public ResponseEntity<Hasil> lapor(@RequestBody Map<String, Object> body, HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        Map<String, Object> data = new HashMap<>(body);
        data.put("jenis_aksi", "LAPOR");
        data.put("id_user", idUser);
        return prosesChat("BLOKIR_LAPOR", data, null).kirim(HttpStatus.CREATED);
    }

    // Method untuk memproses chat
    public Hasil prosesChat(String aksi, Map<String, Object> dataChat, MultipartFile file) {
        if (aksi.equals("BUKA_CHAT")) {
            return bukaRuangChat(dataChat);
        } else if (aksi.equals("KIRIM_PESAN")) {
            return kirimPesanTeks(dataChat);
        } else if (aksi.equals("KIRIM_GAMBAR")) {
            return kirimPesanGambar(dataChat, file);
        } else if (aksi.equals("BLOKIR_LAPOR")) {
            return prosesBlokirLapor(dataChat);
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    // Method untuk membuka ruang chat beserta riwayat pesan
    private Hasil bukaRuangChat(Map<String, Object> data) {
        String idUser = teks(data, "id_user");
        String idTujuan = teks(data, "id_user_tujuan");
        String idPost = teks(data, "id_post");

        if (idUser.isBlank() || idTujuan.isBlank() || idPost.isBlank() || idUser.equals(idTujuan)) {
            return new Hasil(false, "Data percakapan tidak valid", null);
        }
        if (!userRepository.existsById(idUser) || !userRepository.existsById(idTujuan)) {
            return new Hasil(false, "Pengguna tidak ditemukan", null);
        }

        // Cari percakapan, buat baru jika belum ada
        Conversation conversation = getOrCreateConversation(idUser, idTujuan, idPost);

        // Ambil riwayat pesan secara kronologis
        List<Message> pesan = messageRepository
                .findByIdConversationOrderByWaktuKirimAsc(conversation.getIdConversation());

        return new Hasil(true, "Ruang chat dibuka", Map.of("conversation", conversation, "pesan", pesan));
    }

    // Method untuk mengirim pesan teks
    private Hasil kirimPesanTeks(Map<String, Object> data) {
        String idConversation = teks(data, "id_conversation");
        String idPengirim = teks(data, "id_pengirim");
        String isi = teks(data, "teks");

        Optional<Conversation> cari = conversationRepository.findById(idConversation);
        if (cari.isEmpty()) {
            return new Hasil(false, "Percakapan tidak ditemukan", null);
        }
        Conversation conversation = cari.get();

        if (!anggotaPercakapan(conversation, idPengirim)) {
            return new Hasil(false, "Pengirim tidak berada dalam percakapan", null);
        }

        // Pesan tidak dapat dikirim jika salah satu pihak memblokir
        String idPenerima = lawanBicara(conversation, idPengirim);
        if (diblokir(idPengirim, idPenerima)) {
            return new Hasil(false, "Pesan tidak dapat dikirim", null);
        }

        if (isi.isBlank()) {
            return new Hasil(false, "Pesan tidak boleh kosong", null);
        }

        // Simpan pesan
        Message pesan = new Message();
        pesan.setIdMessage(idGenerator.buat("MSG", "message", "id_message"));
        pesan.setIdConversation(idConversation);
        pesan.setIdPengirim(idPengirim);
        pesan.setIsiTeks(isi);
        pesan.setWaktuKirim(LocalDateTime.now());
        messageRepository.save(pesan);

        buatNotifikasiPesan(idPengirim, idPenerima, pesan);

        return new Hasil(true, "Pesan terkirim", pesan);
    }

    // Method untuk mengirim pesan gambar
    private Hasil kirimPesanGambar(Map<String, Object> data, MultipartFile file) {
        String idConversation = teks(data, "id_conversation");
        String idPengirim = teks(data, "id_pengirim");

        Optional<Conversation> cari = conversationRepository.findById(idConversation);
        if (cari.isEmpty()) {
            return new Hasil(false, "Percakapan tidak ditemukan", null);
        }
        Conversation conversation = cari.get();

        if (!anggotaPercakapan(conversation, idPengirim)) {
            return new Hasil(false, "Pengirim tidak berada dalam percakapan", null);
        }

        String idPenerima = lawanBicara(conversation, idPengirim);
        if (diblokir(idPengirim, idPenerima)) {
            return new Hasil(false, "Pesan tidak dapat dikirim", null);
        }

        // Validasi ekstensi (JPG/PNG) dan ukuran (maks. 5 MB)
        if (!validasiFile(file)) {
            return new Hasil(false, "File Ditolak: gunakan gambar JPG atau PNG maksimal 5 MB", null);
        }

        // Simpan file lalu simpan pesan
        String idMessage = idGenerator.buat("MSG", "message", "id_message");
        String namaFile;
        try {
            namaFile = simpanFile(idMessage, file);
        } catch (IOException e) {
            return new Hasil(false, "Gambar gagal disimpan", null);
        }

        Message pesan = new Message();
        pesan.setIdMessage(idMessage);
        pesan.setIdConversation(idConversation);
        pesan.setIdPengirim(idPengirim);
        pesan.setFileGambar(namaFile);
        pesan.setWaktuKirim(LocalDateTime.now());
        messageRepository.save(pesan);

        buatNotifikasiPesan(idPengirim, idPenerima, pesan);

        return new Hasil(true, "Gambar terkirim", pesan);
    }

    // Method untuk memproses blokir atau laporan pengguna
    private Hasil prosesBlokirLapor(Map<String, Object> data) {
        String jenis = teks(data, "jenis_aksi");
        String idUser = teks(data, "id_user");
        String idTarget = teks(data, "id_user_target");

        if (idUser.equals(idTarget) || !userRepository.existsById(idUser)
                || !userRepository.existsById(idTarget)) {
            return new Hasil(false, "Data pengguna tidak valid", null);
        }

        if (jenis.equals("BLOKIR")) {
            return blokirPengguna(idUser, idTarget);
        } else if (jenis.equals("LAPOR")) {
            return laporPengguna(idUser, idTarget, teks(data, "alasan"));
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    // Method untuk memblokir pengguna
    private Hasil blokirPengguna(String idUser, String idTarget) {
        Optional<UserBlock> ada = userBlockRepository.findByIdUserAndIdUserDiblokir(idUser, idTarget);

        // Aktifkan kembali blokir lama atau buat data blokir baru
        UserBlock blokir = ada.orElseGet(() -> {
            UserBlock baru = new UserBlock();
            baru.setIdBlock(idGenerator.buat("BLK", "userblock", "id_block"));
            baru.setIdUser(idUser);
            baru.setIdUserDiblokir(idTarget);
            return baru;
        });
        blokir.setStatusBlokir(true);
        userBlockRepository.save(blokir);

        return new Hasil(true, "Pengguna berhasil diblokir", null);
    }

    // Method untuk melaporkan pengguna
    private Hasil laporPengguna(String idUser, String idTarget, String alasan) {
        if (alasan.isBlank()) {
            return new Hasil(false, "Alasan laporan wajib diisi", null);
        }

        UserReport laporan = new UserReport();
        laporan.setIdReport(idGenerator.buat("RPT", "userreport", "id_report"));
        laporan.setIdUser(idUser);
        laporan.setIdUserDilaporkan(idTarget);
        laporan.setAlasan(alasan);
        userReportRepository.save(laporan);

        return new Hasil(true, "Laporan berhasil dikirim", null);
    }

    // Method untuk mencari atau membuat percakapan
    private Conversation getOrCreateConversation(String idUser, String idTujuan, String idPost) {
        Optional<Conversation> ada = conversationRepository.cariConversation(idUser, idTujuan, idPost);
        if (ada.isPresent()) {
            return ada.get();
        }

        Conversation baru = new Conversation();
        baru.setIdConversation(idGenerator.buat("CVS", "conversation", "id_conversation"));
        baru.setIdUserPengirim(idUser);
        baru.setIdUserTujuan(idTujuan);
        baru.setIdPost(idPost);
        return conversationRepository.save(baru);
    }

    // Method untuk membuat notifikasi pesan baru kepada penerima
    private void buatNotifikasiPesan(String idPengirim, String idPenerima, Message pesan) {
        String username = userRepository.findById(idPengirim).map(User::getUsername).orElse("pengguna");

        notifikasiController.prosesNotifikasi("BUAT_NOTIFIKASI", Map.of(
                "id_user", idPenerima,
                "tipe", "pesan",
                "pesan", "Pesan baru dari " + username,
                "id_referensi", pesan.getIdMessage()));
    }

    // Method untuk validasi file gambar
    private boolean validasiFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > UKURAN_MAKS) {
            return false;
        }
        String tipe = file.getContentType();
        return tipe != null && tipe.startsWith("image/") && EKSTENSI_BOLEH.contains(ekstensi(file));
    }

    // Method untuk menyimpan file gambar ke folder upload
    private String simpanFile(String idMessage, MultipartFile file) throws IOException {
        Path folder = Paths.get(FOLDER_UPLOAD).toAbsolutePath();
        Files.createDirectories(folder);

        String namaFile = idMessage + "." + ekstensi(file);
        file.transferTo(folder.resolve(namaFile));
        return namaFile;
    }

    // Fungsi mengambil ekstensi file dalam huruf kecil
    private String ekstensi(MultipartFile file) {
        String nama = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int titik = nama.lastIndexOf('.');
        return titik < 0 ? "" : nama.substring(titik + 1).toLowerCase();
    }

    // Fungsi memeriksa apakah pengguna anggota percakapan
    private boolean anggotaPercakapan(Conversation conversation, String idUser) {
        return idUser.equals(conversation.getIdUserPengirim()) || idUser.equals(conversation.getIdUserTujuan());
    }

    // Fungsi mengambil lawan bicara dalam percakapan
    private String lawanBicara(Conversation conversation, String idUser) {
        return idUser.equals(conversation.getIdUserPengirim())
                ? conversation.getIdUserTujuan() : conversation.getIdUserPengirim();
    }

    // Fungsi memeriksa blokir aktif di salah satu arah
    private boolean diblokir(String idA, String idB) {
        return userBlockRepository.existsByIdUserAndIdUserDiblokirAndStatusBlokir(idA, idB, true)
                || userBlockRepository.existsByIdUserAndIdUserDiblokirAndStatusBlokir(idB, idA, true);
    }

    // Fungsi mengambil id pengguna yang sedang login dari session
    private String idSesi(HttpSession session) {
        return (String) session.getAttribute("idUser");
    }

    private ResponseEntity<Hasil> belumLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new Hasil(false, "Silakan login terlebih dahulu", null));
    }

    // Fungsi mengambil nilai teks dari body
    private String teks(Map<String, Object> data, String kunci) {
        Object nilai = data.get(kunci);
        return nilai == null ? "" : nilai.toString();
    }
}
