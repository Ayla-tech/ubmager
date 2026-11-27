package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Notifikasi;
import com.example.ubmager.repository.MessageRepository;
import com.example.ubmager.repository.NotifikasiRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifikasi")
@Transactional
public class NotifikasiController {

    private static final Set<String> TIPE_BOLEH = Set.of("pesan", "review", "status");

    private final UserRepository userRepository;
    private final NotifikasiRepository notifikasiRepository;
    private final MessageRepository messageRepository;
    private final IdGenerator idGenerator;
    private final boolean pushAktif = false;

    public NotifikasiController(UserRepository userRepository, NotifikasiRepository notifikasiRepository,
            MessageRepository messageRepository, IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.notifikasiRepository = notifikasiRepository;
        this.messageRepository = messageRepository;
        this.idGenerator = idGenerator;
    }

    // Lihat daftar notifikasi pengguna yang sedang login
    @GetMapping
    public ResponseEntity<Hasil> lihatDaftar(HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        return prosesNotifikasi("LIHAT_DAFTAR", Map.of("id_user", idUser)).kirim(HttpStatus.OK);
    }

    // Buka notifikasi dan tandai sudah dibaca
    @PutMapping("/{idNotifikasi}/baca")
    public ResponseEntity<Hasil> buka(@PathVariable("idNotifikasi") String idNotifikasi, HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        return prosesNotifikasi("BUKA_NOTIFIKASI",
                Map.of("id_notifikasi", idNotifikasi, "id_user", idUser)).kirim(HttpStatus.OK);
    }

    // Tandai semua notifikasi sudah dibaca
    @PutMapping("/baca-semua")
    public ResponseEntity<Hasil> bacaSemua(HttpSession session) {
        String idUser = idSesi(session);
        if (idUser == null) {
            return belumLogin();
        }
        return prosesNotifikasi("TANDAI_SEMUA_DIBACA", Map.of("id_user", idUser)).kirim(HttpStatus.OK);
    }

    // Method untuk memproses notifikasi (BUAT_NOTIFIKASI dipanggil oleh modul lain)
    public Hasil prosesNotifikasi(String aksi, Map<String, Object> dataNotifikasi) {
        if (aksi.equals("BUAT_NOTIFIKASI")) {
            return buatNotifikasi(dataNotifikasi);
        } else if (aksi.equals("LIHAT_DAFTAR")) {
            return lihatDaftarNotifikasi(teks(dataNotifikasi, "id_user"));
        } else if (aksi.equals("BUKA_NOTIFIKASI")) {
            return bukaNotifikasi(teks(dataNotifikasi, "id_notifikasi"), teks(dataNotifikasi, "id_user"));
        } else if (aksi.equals("TANDAI_SEMUA_DIBACA")) {
            return tandaiSemuaDibaca(teks(dataNotifikasi, "id_user"));
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    // Method untuk membuat notifikasi baru
    private Hasil buatNotifikasi(Map<String, Object> data) {
        String idUser = teks(data, "id_user");
        String tipe = teks(data, "tipe");
        String pesan = teks(data, "pesan");
        String idReferensi = teks(data, "id_referensi");

        // Validasi data notifikasi
        if (!userRepository.existsById(idUser) || !TIPE_BOLEH.contains(tipe)
                || pesan.isBlank() || idReferensi.isBlank()) {
            return new Hasil(false, "Data notifikasi tidak valid", null);
        }

        // Simpan notifikasi dengan status belum dibaca
        Notifikasi notifikasi = new Notifikasi();
        notifikasi.setIdNotifikasi(idGenerator.buat("NTF", "notifikasi", "id_notifikasi"));
        notifikasi.setIdUser(idUser);
        notifikasi.setTipe(tipe);
        notifikasi.setPesan(pesan);
        notifikasi.setIdReferensi(idReferensi);
        notifikasi.setUrlTujuan(tentukanUrlTujuan(tipe, idReferensi));
        notifikasi.setDibaca(false);
        notifikasi.setCreatedAt(LocalDateTime.now());
        notifikasiRepository.save(notifikasi);

        if (pushAktif) {
            kirimPemberitahuanEksternal(idUser, pesan);
        }

        // Jumlah belum dibaca dipakai untuk badge counter pada ikon lonceng
        return new Hasil(true, "Notifikasi berhasil dibuat", Map.of("belum_dibaca", hitungBelumDibaca(idUser)));
    }

    // Method untuk menampilkan daftar notifikasi
    private Hasil lihatDaftarNotifikasi(String idUser) {
        if (!userRepository.existsById(idUser)) {
            return new Hasil(false, "Pengguna tidak ditemukan", null);
        }

        // Ambil notifikasi dari yang terbaru
        List<Notifikasi> daftar = notifikasiRepository.findByIdUserOrderByCreatedAtDesc(idUser);
        if (daftar.isEmpty()) {
            return new Hasil(true, "Belum ada notifikasi", daftar);
        }

        return new Hasil(true, "Daftar notifikasi ditemukan",
                Map.of("notifikasi", daftar, "belum_dibaca", hitungBelumDibaca(idUser)));
    }

    // Method untuk membuka notifikasi dan menandainya sudah dibaca
    private Hasil bukaNotifikasi(String idNotifikasi, String idUser) {
        Optional<Notifikasi> cari = notifikasiRepository.findById(idNotifikasi);

        // Notifikasi milik pengguna lain diperlakukan sebagai tidak ditemukan
        if (cari.isEmpty() || !cari.get().getIdUser().equals(idUser)) {
            return new Hasil(false, "Notifikasi tidak ditemukan", null);
        }

        Notifikasi notifikasi = cari.get();
        notifikasi.setDibaca(true);
        notifikasiRepository.save(notifikasi);

        return new Hasil(true, "Notifikasi dibuka",
                Map.of("url_tujuan", notifikasi.getUrlTujuan(), "belum_dibaca", hitungBelumDibaca(idUser)));
    }

    // Method untuk menandai semua notifikasi sudah dibaca
    private Hasil tandaiSemuaDibaca(String idUser) {
        if (!userRepository.existsById(idUser)) {
            return new Hasil(false, "Pengguna tidak ditemukan", null);
        }

        notifikasiRepository.tandaiSemuaDibaca(idUser);
        return new Hasil(true, "Semua notifikasi ditandai sudah dibaca", Map.of("belum_dibaca", 0));
    }

    // Method untuk menentukan halaman tujuan berdasarkan tipe dan id_referensi
    private String tentukanUrlTujuan(String tipe, String idReferensi) {
        if (tipe.equals("pesan")) {
            return messageRepository.findById(idReferensi)
                    .map(pesan -> "/chat/" + pesan.getIdConversation()).orElse("/chat");
        } else if (tipe.equals("review")) {
            return "/profil";
        }
        return "/";
    }

    // Method untuk mengirim push notification atau email
    private void kirimPemberitahuanEksternal(String idUser, String pesan) {
        // Ganti dengan layanan push notification atau email
        System.out.println("Pemberitahuan untuk " + idUser + ": " + pesan);
    }

    // Fungsi menghitung notifikasi yang belum dibaca
    private long hitungBelumDibaca(String idUser) {
        return notifikasiRepository.countByIdUserAndDibaca(idUser, false);
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
