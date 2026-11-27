package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.DataPembayaran;
import com.example.ubmager.entity.Pembayaran;
import com.example.ubmager.entity.Pemesanan;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.repository.DataPembayaranRepository;
import com.example.ubmager.repository.PembayaranRepository;
import com.example.ubmager.repository.PemesananRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganBacaRepository;
import com.example.ubmager.util.IdGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
@RequestMapping("/api/pembayaran")
@Transactional
public class PembayaranController {

    private static final String MENUNGGU_PESANAN = "menunggu";
    private static final String DITERIMA = "diterima";
    private static final String DIBAYAR = "dibayar";
    private static final String MENUNGGU_KONFIRMASI = "menunggu_konfirmasi";
    private static final String PERBAIKAN = "perbaikan";
    private static final String DIKONFIRMASI = "dikonfirmasi";

    private final PesananRepository pesananRepository;
    private final PemesananRepository pemesananRepository;
    private final PembayaranRepository pembayaranRepository;
    private final DataPembayaranRepository dataPembayaranRepository;
    private final PostinganBacaRepository postinganBacaRepository;
    private final IdGenerator idGenerator;

    public PembayaranController(PesananRepository pesananRepository,
                                PemesananRepository pemesananRepository,
                                PembayaranRepository pembayaranRepository,
                                DataPembayaranRepository dataPembayaranRepository,
                                PostinganBacaRepository postinganBacaRepository,
                                IdGenerator idGenerator) {
        this.pesananRepository = pesananRepository;
        this.pemesananRepository = pemesananRepository;
        this.pembayaranRepository = pembayaranRepository;
        this.dataPembayaranRepository = dataPembayaranRepository;
        this.postinganBacaRepository = postinganBacaRepository;
        this.idGenerator = idGenerator;
    }

    @PostMapping
    public ResponseEntity<Hasil> buat(@RequestBody(required = false) Map<String, Object> body) {
        return buatPembayaran(data(body)).kirim(HttpStatus.CREATED);
    }

    @GetMapping("/{idPembayaran}")
    public ResponseEntity<Hasil> lihat(@PathVariable String idPembayaran) {
        return lihatPembayaran(idPembayaran).kirim(HttpStatus.OK);
    }

    @GetMapping("/pesanan/{idPesanan}")
    public ResponseEntity<Hasil> lihatBerdasarkanPesanan(@PathVariable String idPesanan) {
        Optional<Pembayaran> cari = pembayaranRepository.findByIdPesanan(idPesanan);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pembayaran tidak ditemukan", null).kirim(HttpStatus.NOT_FOUND);
        }
        return new Hasil(true, "Pembayaran ditemukan", dataPembayaran(cari.get())).kirim(HttpStatus.OK);
    }

    @PostMapping("/{idPembayaran}/bukti")
    public ResponseEntity<Hasil> simpanBukti(@PathVariable String idPembayaran,
                                             @RequestBody(required = false) Map<String, Object> body) {
        return simpanBuktiPembayaran(idPembayaran, data(body)).kirim(HttpStatus.CREATED);
    }

    @PutMapping("/{idPembayaran}/konfirmasi")
    public ResponseEntity<Hasil> konfirmasi(@PathVariable String idPembayaran,
                                            @RequestBody(required = false) Map<String, Object> body) {
        return konfirmasiPembayaran(idPembayaran, data(body)).kirim(HttpStatus.OK);
    }

    @PutMapping("/{idPembayaran}/perbaikan")
    public ResponseEntity<Hasil> perbaikan(@PathVariable String idPembayaran,
                                           @RequestBody(required = false) Map<String, Object> body) {
        return mintaPerbaikan(idPembayaran, data(body)).kirim(HttpStatus.OK);
    }

    private Hasil buatPembayaran(Map<String, Object> data) {
        String idPesanan = teks(data, "id_pesanan");
        String metode = teks(data, "metode").trim();

        if (idPesanan.isBlank()) {
            return new Hasil(false, "Data pembayaran tidak lengkap", null);
        }

        if (metode.isBlank()) {
            return new Hasil(false, "Metode pembayaran wajib diisi", null);
        }

        if (metode.length() > 50) {
            return new Hasil(false, "Metode pembayaran terlalu panjang", null);
        }

        Optional<Pesanan> cariPesanan = pesananRepository.findById(idPesanan);
        if (cariPesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }

        Optional<Pemesanan> cariPemesanan = pemesananRepository.findByIdPesanan(idPesanan);
        if (cariPemesanan.isEmpty()) {
            return new Hasil(false, "Pemesanan tidak ditemukan", null);
        }

        if (!DITERIMA.equals(cariPemesanan.get().getStatusPesanan())) {
            return new Hasil(false, "Pemesanan belum diterima", null);
        }

        Pesanan pesanan = cariPesanan.get();
        if (!MENUNGGU_PESANAN.equals(pesanan.getStatusPesanan())) {
            return new Hasil(false, "Status pesanan tidak dapat dibayar", null);
        }

        if (pembayaranRepository.existsByIdPesanan(idPesanan)) {
            return new Hasil(false, "Pembayaran sudah ada untuk pesanan ini", null);
        }

        BigDecimal jumlah = pesanan.getTotalHarga();
        if (jumlah == null || jumlah.compareTo(BigDecimal.ZERO) <= 0) {
            return new Hasil(false, "Total harga pesanan tidak tersedia", null);
        }

        Pembayaran pembayaran = new Pembayaran();
        pembayaran.setIdPembayaran(idGenerator.buat("PBY", "pembayaran", "id_pembayaran"));
        pembayaran.setIdPesanan(idPesanan);
        pembayaran.setMetode(metode);
        // Nominal selalu diambil dari PESANAN.total_harga, bukan dari request.
        pembayaran.setJumlah(jumlah);
        pembayaran.setStatus(MENUNGGU_KONFIRMASI);
        pembayaran.setTanggalBayar(LocalDateTime.now());
        pembayaranRepository.save(pembayaran);

        return new Hasil(true, "Pembayaran berhasil dibuat", dataPembayaran(pembayaran));
    }

    private Hasil lihatPembayaran(String idPembayaran) {
        Optional<Pembayaran> cari = pembayaranRepository.findById(idPembayaran);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pembayaran tidak ditemukan", null);
        }
        return new Hasil(true, "Pembayaran ditemukan", dataPembayaran(cari.get()));
    }

    private Hasil simpanBuktiPembayaran(String idPembayaran, Map<String, Object> data) {
        Optional<Pembayaran> cari = pembayaranRepository.findById(idPembayaran);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pembayaran tidak ditemukan", null);
        }

        Pembayaran pembayaran = cari.get();
        if (DIKONFIRMASI.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Pembayaran sudah dikonfirmasi", null);
        }

        if (!MENUNGGU_KONFIRMASI.equals(pembayaran.getStatus()) && !PERBAIKAN.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Pembayaran tidak dapat menerima bukti", null);
        }

        String bukti = teks(data, "bukti_pembayaran").trim();
        if (bukti.isBlank()) {
            return new Hasil(false, "Bukti pembayaran wajib diisi", null);
        }

        if (bukti.length() > 255) {
            return new Hasil(false, "Bukti pembayaran terlalu panjang", null);
        }

        DataPembayaran dataPembayaran = new DataPembayaran();
        dataPembayaran.setIdDataPembayaran(idGenerator.buat("DPY", "datapembayaran", "id_data_pembayaran"));
        dataPembayaran.setIdPembayaran(idPembayaran);
        dataPembayaran.setBuktiPembayaran(bukti);
        dataPembayaran.setTanggalSimpan(LocalDateTime.now());
        dataPembayaranRepository.save(dataPembayaran);

        if (PERBAIKAN.equals(pembayaran.getStatus())) {
            pembayaran.setStatus(MENUNGGU_KONFIRMASI);
            pembayaranRepository.save(pembayaran);
        }

        Map<String, Object> hasil = new LinkedHashMap<>();
        hasil.put("id_data_pembayaran", dataPembayaran.getIdDataPembayaran());
        hasil.put("id_pembayaran", idPembayaran);
        hasil.put("bukti_pembayaran", bukti);
        hasil.put("status", pembayaran.getStatus());
        return new Hasil(true, "Bukti pembayaran berhasil disimpan", hasil);
    }

    private Hasil konfirmasiPembayaran(String idPembayaran, Map<String, Object> data) {
        Optional<Pembayaran> cari = pembayaranRepository.findById(idPembayaran);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pembayaran tidak ditemukan", null);
        }

        Pembayaran pembayaran = cari.get();

        Optional<Pesanan> cariPesanan = pesananRepository.findById(pembayaran.getIdPesanan());
        if (cariPesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }

        Pesanan pesanan = cariPesanan.get();
        Hasil masalah = cekKewenanganPenyedia(pesanan.getIdPostingan(), teks(data, "id_penyedia"));
        if (masalah != null) {
            return masalah;
        }

        if (DIKONFIRMASI.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Pembayaran sudah dikonfirmasi", null);
        }

        if (PERBAIKAN.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Pembayaran masih dalam perbaikan", null);
        }

        if (!MENUNGGU_KONFIRMASI.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Status pembayaran tidak dapat dikonfirmasi", null);
        }

        if (!MENUNGGU_PESANAN.equals(pesanan.getStatusPesanan())) {
            return new Hasil(false, "Status pesanan tidak dapat dikonfirmasi", null);
        }

        pembayaran.setStatus(DIKONFIRMASI);
        pembayaranRepository.save(pembayaran);

        pesanan.setStatusPesanan(DIBAYAR);
        pesananRepository.save(pesanan);

        return new Hasil(true, "Pembayaran berhasil dikonfirmasi",
                Map.of("id_pembayaran", pembayaran.getIdPembayaran(),
                        "status", pembayaran.getStatus(),
                        "status_pesanan", pesanan.getStatusPesanan()));
    }

    private Hasil mintaPerbaikan(String idPembayaran, Map<String, Object> data) {
        Optional<Pembayaran> cari = pembayaranRepository.findById(idPembayaran);
        if (cari.isEmpty()) {
            return new Hasil(false, "Pembayaran tidak ditemukan", null);
        }

        Pembayaran pembayaran = cari.get();

        Optional<Pesanan> cariPesanan = pesananRepository.findById(pembayaran.getIdPesanan());
        if (cariPesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }

        Hasil masalah = cekKewenanganPenyedia(cariPesanan.get().getIdPostingan(), teks(data, "id_penyedia"));
        if (masalah != null) {
            return masalah;
        }

        if (DIKONFIRMASI.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Pembayaran sudah dikonfirmasi", null);
        }

        if (!MENUNGGU_KONFIRMASI.equals(pembayaran.getStatus())) {
            return new Hasil(false, "Status pembayaran tidak dapat dimintai perbaikan", null);
        }

        pembayaran.setStatus(PERBAIKAN);
        pembayaranRepository.save(pembayaran);

        return new Hasil(true, "Permintaan perbaikan bukti berhasil dikirim",
                Map.of("id_pembayaran", pembayaran.getIdPembayaran(),
                        "status", pembayaran.getStatus(),
                        "status_pesanan", cariPesanan.get().getStatusPesanan()));
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
            return new Hasil(false, "Pengguna tidak berwenang atas pembayaran ini", null);
        }

        return null;
    }

    private Map<String, Object> dataPembayaran(Pembayaran pembayaran) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pembayaran", petaPembayaran(pembayaran));
        data.put("riwayat_bukti", dataPembayaranRepository
                .findByIdPembayaranOrderByTanggalSimpanDesc(pembayaran.getIdPembayaran())
                .stream().map(this::petaBukti).toList());
        pesananRepository.findById(pembayaran.getIdPesanan())
                .ifPresent(pesanan -> data.put("pesanan", petaPesanan(pesanan)));
        return data;
    }

    private Map<String, Object> petaPembayaran(Pembayaran pembayaran) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id_pembayaran", pembayaran.getIdPembayaran());
        data.put("id_pesanan", pembayaran.getIdPesanan());
        data.put("metode", pembayaran.getMetode());
        data.put("jumlah", pembayaran.getJumlah());
        data.put("status", pembayaran.getStatus());
        data.put("tanggal_bayar", pembayaran.getTanggalBayar());
        return data;
    }

    private Map<String, Object> petaBukti(DataPembayaran bukti) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id_data_pembayaran", bukti.getIdDataPembayaran());
        data.put("id_pembayaran", bukti.getIdPembayaran());
        data.put("bukti_pembayaran", bukti.getBuktiPembayaran());
        data.put("tanggal_simpan", bukti.getTanggalSimpan());
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

    private String teks(Map<String, Object> data, String kunci) {
        Object nilai = data.get(kunci);
        return nilai == null ? "" : nilai.toString();
    }
}
