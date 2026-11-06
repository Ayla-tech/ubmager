package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.DataLaporan;
import com.example.ubmager.entity.DataReview;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.DataLaporanRepository;
import com.example.ubmager.repository.DataReviewRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
@Transactional
public class ReviewController {

    private final UserRepository userRepository;
    private final PesananRepository pesananRepository;
    private final DataReviewRepository dataReviewRepository;
    private final DataLaporanRepository dataLaporanRepository;
    private final IdGenerator idGenerator;

    public ReviewController(UserRepository userRepository, PesananRepository pesananRepository,
                            DataReviewRepository dataReviewRepository,
                            DataLaporanRepository dataLaporanRepository, IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.pesananRepository = pesananRepository;
        this.dataReviewRepository = dataReviewRepository;
        this.dataLaporanRepository = dataLaporanRepository;
        this.idGenerator = idGenerator;
    }

    @PostMapping
    public ResponseEntity<Hasil> buat(@RequestBody Map<String, Object> body) {
        return prosesReviewProfil("CREATE_REVIEW", body).kirim(HttpStatus.CREATED);
    }

    @PostMapping("/laporan")
    public ResponseEntity<Hasil> lapor(@RequestBody Map<String, Object> body) {
        return prosesReviewProfil("REPORT_REVIEW", body).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/{idTarget}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idTarget) {
        return prosesReviewProfil("READ_REVIEW", Map.of("id_target", idTarget)).kirim(HttpStatus.OK);
    }

    public Hasil prosesReviewProfil(String aksi, Map<String, Object> dataReview) {
        if (aksi.equals("CREATE_REVIEW")) {
            return simpanReview(dataReview);
        } else if (aksi.equals("READ_REVIEW")) {
            return lihatReview(teks(dataReview, "id_target"));
        } else if (aksi.equals("REPORT_REVIEW")) {
            return simpanLaporan(dataReview);
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    private Hasil simpanReview(Map<String, Object> data) {
        String idPesanan = teks(data, "id_pesanan");
        String idReviewer = teks(data, "id_reviewer");
        String idTarget = teks(data, "id_target");

        Optional<Pesanan> cariPesanan = pesananRepository.findById(idPesanan);
        Optional<User> cariTarget = userRepository.findById(idTarget);
        if (cariPesanan.isEmpty() || cariTarget.isEmpty() || !userRepository.existsById(idReviewer)) {
            return new Hasil(false, "Data tidak ditemukan", null);
        }
        Pesanan pesanan = cariPesanan.get();

        if (!pesanan.getStatusPesanan().equals("selesai")) {
            return new Hasil(false, "Pesanan belum selesai", null);
        }

        if (!pesanan.getIdPembeli().equals(idReviewer)) {
            return new Hasil(false, "Review tidak diizinkan", null);
        }

        int rating = angka(teks(data, "rating"));
        if (rating < 1 || rating > 5) {
            return new Hasil(false, "Rating harus antara 1 sampai 5", null);
        }

        if (dataReviewRepository.existsByIdPesananAndIdReviewer(idPesanan, idReviewer)) {
            return new Hasil(false, "Review sudah diberikan", null);
        }

        String komentar = teks(data, "komentar");
        DataReview review = new DataReview();
        review.setIdReview(idGenerator.buat("REV", "datareview", "id_review"));
        review.setIdPesanan(idPesanan);
        review.setIdReviewer(idReviewer);
        review.setIdTarget(idTarget);
        review.setRating((short) rating);
        review.setKomentar(komentar.isBlank() ? null : komentar);
        review.setTanggalReview(LocalDateTime.now());
        dataReviewRepository.save(review);

        double rata = Math.round(dataReviewRepository.hitungRataRata(idTarget) * 10) / 10.0;

        User target = cariTarget.get();
        target.setRataRataRating((float) rata);
        target.setBadgeKepercayaan(rata >= 4.5 ? "Penjual Terpercaya" : "Pengguna Terpercaya Baru");
        userRepository.save(target);

        kirimNotifikasi(idTarget);

        return new Hasil(true, "Review berhasil disimpan", null);
    }

    private Hasil lihatReview(String idTarget) {
        Optional<User> cari = userRepository.findById(idTarget);

        if (cari.isEmpty()) {
            return new Hasil(false, "Profil tidak ditemukan", null);
        }
        User user = cari.get();

        List<DataReview> review = dataReviewRepository.findByIdTargetOrderByTanggalReviewDesc(idTarget);

        Map<String, Object> profil = Map.of(
                "username", user.getUsername(),
                "rata_rata_rating", user.getRataRataRating(),
                "badge_kepercayaan", user.getBadgeKepercayaan());

        return new Hasil(true, "Data review ditemukan", Map.of("profil", profil, "review", review));
    }

    private Hasil simpanLaporan(Map<String, Object> data) {
        String idReview = teks(data, "id_review");
        String idPelapor = teks(data, "id_pelapor");

        if (!dataReviewRepository.existsById(idReview) || !userRepository.existsById(idPelapor)) {
            return new Hasil(false, "Data tidak ditemukan", null);
        }

        String alasan = teks(data, "alasan_laporan");
        if (alasan.isBlank()) {
            return new Hasil(false, "Alasan laporan wajib diisi", null);
        }

        DataLaporan laporan = new DataLaporan();
        laporan.setIdLaporan(idGenerator.buat("LPR", "datalaporan", "id_laporan"));
        laporan.setIdReview(idReview);
        laporan.setIdPelapor(idPelapor);
        laporan.setAlasanLaporan(alasan);
        laporan.setStatusLaporan("menunggu");
        laporan.setTanggalLaporan(LocalDateTime.now());
        dataLaporanRepository.save(laporan);

        return new Hasil(true, "Laporan berhasil dikirim", null);
    }

    private void kirimNotifikasi(String idUser) {
        // Terhubung ke modul notifikasi
    }

    private int angka(String nilai) {
        try {
            return Integer.parseInt(nilai.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String teks(Map<String, Object> data, String kunci) {
        Object nilai = data.get(kunci);
        return nilai == null ? "" : nilai.toString();
    }
}
