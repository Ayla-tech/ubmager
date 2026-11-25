package com.example.ubmager.service;

import com.example.ubmager.dto.LaporanItem;
import com.example.ubmager.dto.PesananItem;
import com.example.ubmager.dto.PostinganItem;
import com.example.ubmager.dto.ProfilTampil;
import com.example.ubmager.dto.ReviewItem;
import com.example.ubmager.entity.DataLaporan;
import com.example.ubmager.entity.DataReview;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.entity.Profil;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.DataLaporanRepository;
import com.example.ubmager.repository.DataReviewRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.ProfilRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.Teks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

// Menyusun data untuk View (Thymeleaf) dari beberapa tabel
@Service
public class TampilanService {

    private final UserRepository userRepository;
    private final ProfilRepository profilRepository;
    private final PostinganRepository postinganRepository;
    private final PesananRepository pesananRepository;
    private final DataReviewRepository dataReviewRepository;
    private final DataLaporanRepository dataLaporanRepository;

    public TampilanService(UserRepository userRepository, ProfilRepository profilRepository,
                           PostinganRepository postinganRepository, PesananRepository pesananRepository,
                           DataReviewRepository dataReviewRepository, DataLaporanRepository dataLaporanRepository) {
        this.userRepository = userRepository;
        this.profilRepository = profilRepository;
        this.postinganRepository = postinganRepository;
        this.pesananRepository = pesananRepository;
        this.dataReviewRepository = dataReviewRepository;
        this.dataLaporanRepository = dataLaporanRepository;
    }

    // ---------- Beranda ----------
    public List<PostinganItem> beranda() {
        List<Postingan> daftar = postinganRepository.findByStatusAktifTrueOrderByCreatedAtDesc();
        Set<String> idPenjual = daftar.stream().map(Postingan::getIdPenjual).collect(Collectors.toSet());
        Map<String, User> users = petaUser(idPenjual);
        Map<String, Profil> profil = petaProfil(idPenjual);

        List<PostinganItem> hasil = new ArrayList<>();
        for (Postingan p : daftar) {
            User penjual = users.get(p.getIdPenjual());
            String nama = namaDari(penjual, profil.get(p.getIdPenjual()));
            float rating = penjual == null ? 0f : penjual.getRataRataRating();
            hasil.add(new PostinganItem(p.getIdPostingan(), p.getJudul(), p.getDeskripsi(),
                    Teks.rupiah(p.getHarga()), p.getKategori(), p.getIdPenjual(), nama, Teks.inisial(nama),
                    Teks.rating(rating), Math.round(rating),
                    penjual != null && "Penjual Terpercaya".equals(penjual.getBadgeKepercayaan()),
                    rating > 0f));
        }
        return hasil;
    }

    // ---------- Profil ----------
    public ProfilTampil profil(String idUser) {
        User user = userRepository.findById(idUser).orElse(null);
        Profil profil = profilRepository.findByIdUser(idUser).orElse(null);
        if (user == null || profil == null) {
            return null;
        }
        String foto = profil.getFotoProfil();
        String fotoUrl = (foto == null || foto.isBlank() || foto.equals("default.jpg")) ? null : foto;
        int jumlah = dataReviewRepository.cariTampil(idUser).size();
        float rating = user.getRataRataRating();
        return new ProfilTampil(user.getIdUser(), user.getUsername(), user.getEmailAtauHp(),
                profil.getNamaLengkap(), fotoUrl, profil.getBio(), Teks.rating(rating), Math.round(rating),
                user.getBadgeKepercayaan(), Teks.inisial(profil.getNamaLengkap()), jumlah,
                Teks.tanggal(user.getCreatedAt()), "Penjual Terpercaya".equals(user.getBadgeKepercayaan()));
    }

    // ---------- Review ----------
    public List<ReviewItem> reviewUntuk(String idTarget) {
        List<DataReview> daftar = dataReviewRepository.cariTampil(idTarget);
        Set<String> idReviewer = daftar.stream().map(DataReview::getIdReviewer).collect(Collectors.toSet());
        Map<String, User> users = petaUser(idReviewer);
        Map<String, Profil> profil = petaProfil(idReviewer);

        List<ReviewItem> hasil = new ArrayList<>();
        for (DataReview r : daftar) {
            User u = users.get(r.getIdReviewer());
            String nama = namaDari(u, profil.get(r.getIdReviewer()));
            hasil.add(new ReviewItem(r.getIdReview(), r.getIdReviewer(), u == null ? "-" : u.getUsername(),
                    nama, Teks.inisial(nama), r.getRating(), r.getKomentar(), Teks.tanggal(r.getTanggalReview())));
        }
        return hasil;
    }

    public Set<String> reviewSudahDilapor(String idPelapor) {
        return dataLaporanRepository.findByIdPelapor(idPelapor).stream()
                .map(DataLaporan::getIdReview).collect(Collectors.toSet());
    }

    // ---------- Pesanan ----------
    public List<PesananItem> pesananSaya(String idUser) {
        List<Pesanan> daftar = pesananRepository.findByIdPembeliOrderByIdPesananDesc(idUser);
        Set<String> sudahReview = dataReviewRepository.findByIdReviewer(idUser).stream()
                .map(DataReview::getIdPesanan).collect(Collectors.toSet());
        return susunPesanan(daftar, sudahReview);
    }

    // Null jika pesanan tidak boleh direview oleh user ini
    public PesananItem pesananUntukReview(String idPesanan, String idUser) {
        Pesanan pesanan = pesananRepository.findById(idPesanan).orElse(null);
        if (pesanan == null || !pesanan.getIdPembeli().equals(idUser) || !pesanan.getStatusPesanan().equals("selesai")
                || dataReviewRepository.existsByIdPesananAndIdReviewer(idPesanan, idUser)) {
            return null;
        }
        List<PesananItem> hasil = susunPesanan(List.of(pesanan), new HashSet<>());
        return hasil.get(0);
    }

    private List<PesananItem> susunPesanan(List<Pesanan> daftar, Set<String> sudahReview) {
        Set<String> idPost = daftar.stream().map(Pesanan::getIdPostingan).collect(Collectors.toSet());
        Map<String, Postingan> posts = postinganRepository.findAllById(idPost).stream()
                .collect(Collectors.toMap(Postingan::getIdPostingan, p -> p));
        Set<String> idPenjual = posts.values().stream().map(Postingan::getIdPenjual).collect(Collectors.toSet());
        Map<String, User> users = petaUser(idPenjual);
        Map<String, Profil> profil = petaProfil(idPenjual);

        List<PesananItem> hasil = new ArrayList<>();
        for (Pesanan p : daftar) {
            Postingan post = posts.get(p.getIdPostingan());
            String judul = post == null ? "Postingan tidak tersedia" : post.getJudul();
            String harga = post == null ? "-" : Teks.rupiah(post.getHarga());
            String idJual = post == null ? "" : post.getIdPenjual();
            String nama = post == null ? "-" : namaDari(users.get(idJual), profil.get(idJual));
            hasil.add(new PesananItem(p.getIdPesanan(), judul, harga, p.getStatusPesanan(), idJual, nama,
                    sudahReview.contains(p.getIdPesanan())));
        }
        return hasil;
    }

    // ---------- Admin ----------
    public List<LaporanItem> laporan(String status) {
        List<DataLaporan> daftar = List.of("menunggu", "diproses", "disetujui", "ditolak").contains(status)
                ? dataLaporanRepository.findByStatusLaporanOrderByTanggalLaporanDesc(status)
                : dataLaporanRepository.findAllByOrderByTanggalLaporanDesc();

        Set<String> idReview = daftar.stream().map(DataLaporan::getIdReview).collect(Collectors.toSet());
        Map<String, DataReview> reviews = dataReviewRepository.findAllById(idReview).stream()
                .collect(Collectors.toMap(DataReview::getIdReview, r -> r));

        Set<String> idUser = new HashSet<>();
        for (DataLaporan l : daftar) {
            idUser.add(l.getIdPelapor());
        }
        for (DataReview r : reviews.values()) {
            idUser.add(r.getIdReviewer());
            idUser.add(r.getIdTarget());
        }
        Map<String, User> users = petaUser(idUser);

        List<LaporanItem> hasil = new ArrayList<>();
        for (DataLaporan l : daftar) {
            DataReview r = reviews.get(l.getIdReview());
            String st = l.getStatusLaporan();
            hasil.add(new LaporanItem(l.getIdLaporan(), st, l.getAlasanLaporan(), Teks.tanggal(l.getTanggalLaporan()),
                    username(users, l.getIdPelapor()), l.getIdReview(),
                    r == null ? 0 : r.getRating(), r == null ? "" : r.getKomentar(),
                    r == null ? "-" : username(users, r.getIdReviewer()),
                    r == null ? "-" : username(users, r.getIdTarget()),
                    r == null ? "-" : Teks.tanggal(r.getTanggalReview()),
                    st.equals("disetujui") || st.equals("ditolak")));
        }
        return hasil;
    }

    public Map<String, Long> hitungLaporan() {
        Map<String, Long> hasil = new LinkedHashMap<>();
        long total = 0;
        for (String st : List.of("menunggu", "diproses", "disetujui", "ditolak")) {
            long n = dataLaporanRepository.countByStatusLaporan(st);
            hasil.put(st, n);
            total += n;
        }
        hasil.put("semua", total);
        return hasil;
    }

    // ---------- Helper ----------
    private Map<String, User> petaUser(Collection<String> ids) {
        Map<String, User> peta = new HashMap<>();
        if (ids.isEmpty()) {
            return peta;
        }
        for (User u : userRepository.findAllById(ids)) {
            peta.put(u.getIdUser(), u);
        }
        return peta;
    }

    private Map<String, Profil> petaProfil(Collection<String> ids) {
        Map<String, Profil> peta = new HashMap<>();
        if (ids.isEmpty()) {
            return peta;
        }
        for (Profil p : profilRepository.findByIdUserIn(ids)) {
            peta.put(p.getIdUser(), p);
        }
        return peta;
    }

    private String namaDari(User user, Profil profil) {
        if (profil != null) {
            return profil.getNamaLengkap();
        }
        return user == null ? "Pengguna" : user.getUsername();
    }

    private String username(Map<String, User> users, String id) {
        User u = users.get(id);
        return u == null ? "-" : u.getUsername();
    }
}
