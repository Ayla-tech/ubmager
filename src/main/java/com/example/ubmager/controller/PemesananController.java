package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Pembayaran;
import com.example.ubmager.entity.Pemesanan;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.repository.PembayaranRepository;
import com.example.ubmager.repository.PemesananRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganBacaRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pemesanan")
@Transactional
public class PemesananController {

    private static final String MENUNGGU = "menunggu";
    private static final String DITERIMA = "diterima";
    private static final String DITOLAK = "ditolak";
    private static final String SELESAI = "selesai";
    private static final String DIBATALKAN = "dibatalkan";
    private static final String DIKONFIRMASI = "dikonfirmasi";

    private final UserRepository userRepository;
    private final PesananRepository pesananRepository;
    private final PemesananRepository pemesananRepository;
    private final PembayaranRepository pembayaranRepository;
    private final PostinganBacaRepository postinganBacaRepository;
    private final IdGenerator idGenerator;

    public PemesananController(UserRepository userRepository, PesananRepository pesananRepository,
                               PemesananRepository pemesananRepository,
                               PembayaranRepository pembayaranRepository,
                               PostinganBacaRepository postinganBacaRepository,
                               IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.pesananRepository = pesananRepository;
        this.pemesananRepository = pemesananRepository;
        this.pembayaranRepository = pembayaranRepository;
        this.postinganBacaRepository = postinganBacaRepository;
        this.idGenerator = idGenerator;
    }

    @PostMapping
    public ResponseEntity<Hasil> buat(@RequestBody(required = false) Map<String, Object> body) {
        return buatPemesanan(data(body)).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/{idPemesanan}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idPemesanan) {
        return lihatPemesanan(idPemesanan).kirim(HttpStatus.OK);
    }

    @GetMapping("/pembeli/{idPembeli}")
    public ResponseEntity<Hasil> daftarPembeli(@PathVariable String idPembeli) {
        return daftarPemesananPembeli(idPembeli).kirim(HttpStatus.OK);
    }

    @GetMapping("/penyedia/{idPenyedia}")
    public ResponseEntity<Hasil> daftarPenyedia(@PathVariable String idPenyedia) {
        return daftarPemesananPenyedia(idPenyedia).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPemesanan}/terima")
    public ResponseEntity<Hasil> terima(@PathVariable String idPemesanan,
                                        @RequestBody(required = false) Map<String, Object> body) {
        return terimaPemesanan(idPemesanan, data(body)).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPemesanan}/tolak")
    public ResponseEntity<Hasil> tolak(@PathVariable String idPemesanan,
                                       @RequestBody(required = false) Map<String, Object> body) {
        return tolakPemesanan(idPemesanan, data(body)).kirim(HttpStatus.OK);
    }

    @PostMapping("/{idPemesanan}/selesai")
    public ResponseEntity<Hasil> selesai(@PathVariable String idPemesanan,
                                         @RequestBody(required = false) Map<String, Object> body) {
        return selesaikanPemesanan(idPemesanan, data(body)).kirim(HttpStatus.OK);
    }

    private Hasil buatPemesanan(Map<String, Object> data) {
        String idPembeli = teks(data, "id_pembeli");
        String idPostingan = teks(data, "id_postingan");

        if (idPembeli.isBlank() || idPostingan.isBlank()) {
            return new Hasil(false, "Data pemesanan tidak lengkap", null);
        }

        if (!userRepository.existsById(idPembeli)) {
            return new Hasil(false, "Pembeli tidak ditemukan", null);
        }

        Optional<PostinganBacaRepository.Info> cariPostingan = postinganBacaRepository.cari(idPostingan);
        if (cariPostingan.isEmpty()) {
            return new Hasil(false, "Postingan tidak ditemukan", null);
        }

        PostinganBacaRepository.Info postingan = cariPostingan.get();
        if (!postingan.statusAktif()) {
            return new Hasil(false, "Postingan tidak aktif", null);
        }

        int jumlah = angka(teks(data, "jumlah"));
        if (jumlah <= 0) {
            return new Hasil(false, "Jumlah harus lebih dari 0", null);
        }

        if (postingan.harga() == null || postingan.harga().compareTo(BigDecimal.ZERO) <= 0) {
            return new Hasil(false, "Harga postingan tidak valid", null);
        }

        String teksWaktu = teks(data, "waktu_pesanan").trim();
        LocalDateTime waktuPesanan = null;
        if (!teksWaktu.isBlank()) {
            try {
                waktuPesanan = LocalDateTime.parse(teksWaktu.replace(' ', 'T'));
            } catch (DateTimeParseException e) {
                return new Hasil(false,
                        "Format waktu_pesanan tidak valid. Contoh: 2026-12-01T10:00:00", null);
            }
        }

        // Harga selalu diambil dari POSTINGAN, bukan dari request.
        BigDecimal totalHarga = postingan.harga()
                .multiply(BigDecimal.valueOf(jumlah))
                .setScale(2, RoundingMode.HALF_UP);
        LocalDateTime sekarang = LocalDateTime.now();

        Pesanan pesanan = new Pesanan();
        pesanan.setIdPesanan(idGenerator.buat("PSN", "pesanan", "id_pesanan"));
        pesanan.setIdPembeli(idPembeli);
        pesanan.setIdPostingan(idPostingan);
        pesanan.setStatusPesanan(MENUNGGU);
        pesanan.setTanggalPesan(sekarang);
        pesanan.setTotalHarga(totalHarga);
        pesananRepository.save(pesanan);

        Pemesanan pemesanan = new Pemesanan();
        pemesanan.setIdPemesanan(idGenerator.buat("PMS", "pemesanan", "id_pemesanan"));
        pemesanan.setIdPesanan(pesanan.getIdPesanan());
        pemesanan.setIdPostingan(idPostingan);
        pemesanan.setIdPembeli(idPembeli);
        pemesanan.setJumlah(jumlah);
        pemesanan.setWaktuPesanan(waktuPesanan);
        pemesanan.setLokasi(teksAtauNull(data, "lokasi"));
        pemesanan.setCatatan(teksAtauNull(data, "catatan"));
        pemesanan.setStatusPesanan(MENUNGGU);
        pemesanan.setCreatedAt(sekarang);
        pemesananRepository.save(pemesanan);

        return new Hasil(true, "Pemesanan berhasil dibuat", dataPemesanan(pemesanan, pesanan));
    }

    private Hasil lihatPemesanan(String idPemesanan) {
        Optional<Pemesanan> cari = pemesananRepository.findById(idPemesanan);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pemesanan tidak ditemukan", null);
        }

        Pemesanan pemesanan = cari.get();
        Pesanan pesanan = pesananRepository.findById(pemesanan.getIdPesanan()).orElse(null);
        return new Hasil(true, "Pemesanan ditemukan", dataPemesanan(pemesanan, pesanan));
    }

    private Hasil daftarPemesananPembeli(String idPembeli) {
        if (!userRepository.existsById(idPembeli)) {
            return new Hasil(false, "Pembeli tidak ditemukan", null);
        }

        List<Pemesanan> daftar = pemesananRepository.findByIdPembeliOrderByCreatedAtDesc(idPembeli);
        return new Hasil(true, "Daftar pemesanan pembeli ditemukan",
                daftar.stream().map(this::petaPemesanan).toList());
    }

    private Hasil daftarPemesananPenyedia(String idPenyedia) {
        if (!userRepository.existsById(idPenyedia)) {
            return new Hasil(false, "Penyedia tidak ditemukan", null);
        }

        List<String> idPostingan = postinganBacaRepository.cariIdByPenjual(idPenyedia);
        if (idPostingan.isEmpty()) {
            return new Hasil(true, "Daftar pemesanan penyedia ditemukan", List.of());
        }

        List<Pemesanan> daftar = pemesananRepository.findByIdPostinganInOrderByCreatedAtDesc(idPostingan);
        return new Hasil(true, "Daftar pemesanan penyedia ditemukan",
                daftar.stream().map(this::petaPemesanan).toList());
    }

    private Hasil terimaPemesanan(String idPemesanan, Map<String, Object> data) {
        Optional<Pemesanan> cari = pemesananRepository.findById(idPemesanan);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pemesanan tidak ditemukan", null);
        }

        Pemesanan pemesanan = cari.get();

        Hasil masalah = cekKewenanganPenyedia(pemesanan.getIdPostingan(), teks(data, "id_penyedia"));
        if (masalah != null) {
            return masalah;
        }

        if (!MENUNGGU.equals(pemesanan.getStatusPesanan())) {
            return new Hasil(false, "Pemesanan tidak dapat diterima karena statusnya bukan menunggu", null);
        }

        Optional<Pesanan> cariPesanan = pesananRepository.findById(pemesanan.getIdPesanan());
        if (cariPesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }

        Pesanan pesanan = cariPesanan.get();
        if (!MENUNGGU.equals(pesanan.getStatusPesanan())) {
            return new Hasil(false, "Status pesanan tidak dapat menerima pemesanan", null);
        }

        pemesanan.setStatusPesanan(DITERIMA);
        pemesananRepository.save(pemesanan);

        return new Hasil(true, "Pemesanan berhasil diterima",
                Map.of("id_pemesanan", pemesanan.getIdPemesanan(),
                        "status_pemesanan", pemesanan.getStatusPesanan(),
                        "status_pesanan", pesanan.getStatusPesanan()));
    }

    private Hasil tolakPemesanan(String idPemesanan, Map<String, Object> data) {
        Optional<Pemesanan> cari = pemesananRepository.findById(idPemesanan);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pemesanan tidak ditemukan", null);
        }

        Pemesanan pemesanan = cari.get();

        Hasil masalah = cekKewenanganPenyedia(pemesanan.getIdPostingan(), teks(data, "id_penyedia"));
        if (masalah != null) {
            return masalah;
        }

        if (!MENUNGGU.equals(pemesanan.getStatusPesanan())) {
            return new Hasil(false, "Pemesanan tidak dapat ditolak karena statusnya bukan menunggu", null);
        }

        if (pembayaranRepository.existsByIdPesanan(pemesanan.getIdPesanan())) {
            return new Hasil(false, "Pemesanan sudah memiliki pembayaran", null);
        }

        Optional<Pesanan> cariPesanan = pesananRepository.findById(pemesanan.getIdPesanan());
        if (cariPesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }

        pemesanan.setStatusPesanan(DITOLAK);
        pemesananRepository.save(pemesanan);

        Pesanan pesanan = cariPesanan.get();
        pesanan.setStatusPesanan(DIBATALKAN);
        pesananRepository.save(pesanan);

        return new Hasil(true, "Pemesanan berhasil ditolak",
                Map.of("id_pemesanan", pemesanan.getIdPemesanan(),
                        "status_pemesanan", pemesanan.getStatusPesanan(),
                        "status_pesanan", pesanan.getStatusPesanan()));
    }

    private Hasil selesaikanPemesanan(String idPemesanan, Map<String, Object> data) {
        Optional<Pemesanan> cari = pemesananRepository.findById(idPemesanan);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pemesanan tidak ditemukan", null);
        }

        Pemesanan pemesanan = cari.get();

        Hasil masalah = cekKewenanganPenyedia(pemesanan.getIdPostingan(), teks(data, "id_penyedia"));
        if (masalah != null) {
            return masalah;
        }

        if (!DITERIMA.equals(pemesanan.getStatusPesanan())) {
            return new Hasil(false, "Pemesanan belum diterima", null);
        }

        Optional<Pesanan> cariPesanan = pesananRepository.findById(pemesanan.getIdPesanan());
        if (cariPesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }

        Pesanan pesanan = cariPesanan.get();
        if (SELESAI.equals(pesanan.getStatusPesanan())) {
            return new Hasil(false, "Pesanan sudah selesai", null);
        }

        Optional<Pembayaran> cariPembayaran = pembayaranRepository.findByIdPesanan(pemesanan.getIdPesanan());
        if (cariPembayaran.isEmpty()) {
            return new Hasil(false, "Pembayaran tidak ditemukan", null);
        }

        Pembayaran pembayaran = cariPembayaran.get();
        if (!DIKONFIRMASI.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Pembayaran belum dikonfirmasi", null);
        }

        pesanan.setStatusPesanan(SELESAI);
        pesananRepository.save(pesanan);

        return new Hasil(true, "Layanan berhasil diselesaikan",
                Map.of("id_pemesanan", pemesanan.getIdPemesanan(),
                        "status_pemesanan", pemesanan.getStatusPesanan(),
                        "status_pesanan", pesanan.getStatusPesanan()));
    }

    /**
     * Pemeriksaan wajib penyedia berdasarkan pemilik POSTINGAN.
     * id_penyedia wajib dikirim; identitas ini masih berasal dari request sehingga
     * belum merupakan autentikasi penuh sampai Spring Security diterapkan.
     */
    private Hasil cekKewenanganPenyedia(String idPostingan, String idPenyedia) {
        if (idPenyedia.isBlank()) {
            return new Hasil(false, "ID penyedia wajib dikirim", null);
        }

        Optional<PostinganBacaRepository.Info> cariPostingan = postinganBacaRepository.cari(idPostingan);
        if (cariPostingan.isEmpty()) {
            return new Hasil(false, "Postingan tidak ditemukan", null);
        }

        if (!idPenyedia.equals(cariPostingan.get().idPenjual())) {
            return new Hasil(false, "Pengguna tidak berwenang atas pemesanan ini", null);
        }

        return null;
    }

    private Map<String, Object> dataPemesanan(Pemesanan pemesanan, Pesanan pesanan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pemesanan", petaPemesanan(pemesanan));
        if (pesanan != null) {
            data.put("pesanan", petaPesanan(pesanan));
        }
        return data;
    }

    private Map<String, Object> petaPemesanan(Pemesanan pemesanan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id_pemesanan", pemesanan.getIdPemesanan());
        data.put("id_pesanan", pemesanan.getIdPesanan());
        data.put("id_postingan", pemesanan.getIdPostingan());
        data.put("id_pembeli", pemesanan.getIdPembeli());
        data.put("jumlah", pemesanan.getJumlah());
        data.put("waktu_pesanan", pemesanan.getWaktuPesanan());
        data.put("lokasi", pemesanan.getLokasi());
        data.put("catatan", pemesanan.getCatatan());
        data.put("status_pesanan", pemesanan.getStatusPesanan());
        data.put("created_at", pemesanan.getCreatedAt());
        return data;
    }

    private Map<String, Object> petaPesanan(Pesanan pesanan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id_pesanan", pesanan.getIdPesanan());
        data.put("id_pembeli", pesanan.getIdPembeli());
        data.put("id_postingan", pesanan.getIdPostingan());
        data.put("status_pesanan", pesanan.getStatusPesanan());
        data.put("tanggal_pesan", pesanan.getTanggalPesan());
        data.put("total_harga", pesanan.getTotalHarga());
        return data;
    }

    private Map<String, Object> data(Map<String, Object> body) {
        return body == null ? Map.of() : body;
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

    private String teksAtauNull(Map<String, Object> data, String kunci) {
        String nilai = teks(data, kunci).trim();
        return nilai.isBlank() ? null : nilai;
    }
}
