package com.example.ubmager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.UserRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PemesananControllerTest {

    private static final String PEMBELI = "USR001";
    private static final String PENYEDIA = "USR002";
    private static final String ORANG_LAIN = "USR003";
    private static final String POSTINGAN_AKTIF = "PST001";
    private static final String POSTINGAN_NONAKTIF = "PST002";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PesananRepository pesananRepository;

    @BeforeEach
    void siapkanData() {
        simpanUser(PEMBELI, "pembeli_uji");
        simpanUser(PENYEDIA, "penyedia_uji");
        simpanUser(ORANG_LAIN, "orang_lain_uji");

        simpanPostingan(POSTINGAN_AKTIF, PENYEDIA, "50000.00", true);
        simpanPostingan(POSTINGAN_NONAKTIF, PENYEDIA, "75000.00", false);
    }

    @Test
    void membuatPemesananValid() throws Exception {
        JsonNode hasil = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 2);

        assertThat(hasil.path("pemesanan").path("status_pesanan").asText()).isEqualTo("menunggu");
        assertThat(hasil.path("pesanan").path("status_pesanan").asText()).isEqualTo("menunggu");
        assertThat(hasil.path("pemesanan").path("id_pemesanan").asText()).startsWith("PMS");
        assertThat(hasil.path("pesanan").path("id_pesanan").asText()).startsWith("PSN");
        assertThat(hasil.path("pesanan").path("id_pembeli").asText()).isEqualTo(PEMBELI);
        assertThat(hasil.path("pemesanan").path("waktu_pesanan").isNull()).isTrue();
    }

    @Test
    void bodyKosongDitolak() throws Exception {
        mockMvc.perform(post("/api/pemesanan").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.berhasil").value(false));

        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.berhasil").value(false));
    }

    @Test
    void pembeliTidakDitemukan() throws Exception {
        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("USR999", POSTINGAN_AKTIF, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pesan").value("Pembeli tidak ditemukan"));
    }

    @Test
    void postinganTidakDitemukan() throws Exception {
        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PEMBELI, "PST999", 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pesan").value("Postingan tidak ditemukan"));
    }

    @Test
    void postinganTidakAktifDitolak() throws Exception {
        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PEMBELI, POSTINGAN_NONAKTIF, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Postingan tidak aktif"));
    }

    @Test
    void jumlahNolAtauNegatifDitolak() throws Exception {
        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PEMBELI, POSTINGAN_AKTIF, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Jumlah harus lebih dari 0"));

        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PEMBELI, POSTINGAN_AKTIF, -3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Jumlah harus lebih dari 0"));
    }

    @Test
    void totalHargaDihitungDariHargaPostingan() throws Exception {
        // total_harga palsu pada request harus diabaikan
        String isi = """
                {"id_pembeli":"%s","id_postingan":"%s","jumlah":3,"total_harga":1}
                """.formatted(PEMBELI, POSTINGAN_AKTIF);

        JsonNode data = kirimPost("/api/pemesanan", isi, 201);

        BigDecimal total = new BigDecimal(data.path("pesanan").path("total_harga").asText());
        assertThat(total).isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    void melihatDetailPemesanan() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 2);
        String idPemesanan = dibuat.path("pemesanan").path("id_pemesanan").asText();

        mockMvc.perform(get("/api/pemesanan/" + idPemesanan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.berhasil").value(true))
                .andExpect(jsonPath("$.data.pemesanan.id_pemesanan").value(idPemesanan))
                .andExpect(jsonPath("$.data.pesanan.id_pembeli").value(PEMBELI));
    }

    @Test
    void melihatDaftarPemesananPembeli() throws Exception {
        buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 2);

        mockMvc.perform(get("/api/pemesanan/pembeli/" + PEMBELI))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void melihatDaftarPemesananPenyedia() throws Exception {
        buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);

        mockMvc.perform(get("/api/pemesanan/penyedia/" + PENYEDIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id_postingan").value(POSTINGAN_AKTIF));

        mockMvc.perform(get("/api/pemesanan/penyedia/" + ORANG_LAIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void menerimaPemesananTidakMengubahStatusPesanan() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idPemesanan = dibuat.path("pemesanan").path("id_pemesanan").asText();
        String idPesanan = dibuat.path("pesanan").path("id_pesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_pemesanan").value("diterima"))
                .andExpect(jsonPath("$.data.status_pesanan").value("menunggu"));

        Pesanan pesanan = pesananRepository.findById(idPesanan).orElseThrow();
        assertThat(pesanan.getStatusPesanan()).isEqualTo("menunggu");
    }

    @Test
    void menolakPemesananMembatalkanPesanan() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idPemesanan = dibuat.path("pemesanan").path("id_pemesanan").asText();
        String idPesanan = dibuat.path("pesanan").path("id_pesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/tolak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_pemesanan").value("ditolak"))
                .andExpect(jsonPath("$.data.status_pesanan").value("dibatalkan"));

        Pesanan pesanan = pesananRepository.findById(idPesanan).orElseThrow();
        assertThat(pesanan.getStatusPesanan()).isEqualTo("dibatalkan");
    }

    @Test
    void transisiStatusTidakDiperbolehkanDitolak() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idPemesanan = dibuat.path("pemesanan").path("id_pemesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk());

        // sudah diterima, tidak boleh ditolak
        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/tolak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.berhasil").value(false));

        // sudah diterima, tidak boleh diterima dua kali
        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void penyediaTidakSesuaiTidakBerwenang() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idPemesanan = dibuat.path("pemesanan").path("id_pemesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + ORANG_LAIN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pengguna tidak berwenang atas pemesanan ini"));
    }

    @Test
    void tanpaIdPenyediaDitolak() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idTerima = dibuat.path("pemesanan").path("id_pemesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idTerima + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));

        mockMvc.perform(put("/api/pemesanan/" + idTerima + "/terima")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));

        JsonNode ditolak = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idTolak = ditolak.path("pemesanan").path("id_pemesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idTolak + "/tolak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));

        JsonNode diselesaikan = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idSelesai = diselesaikan.path("pemesanan").path("id_pemesanan").asText();
        terima(idSelesai);

        mockMvc.perform(post("/api/pemesanan/" + idSelesai + "/selesai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));
    }

    @Test
    void idPenyediaSalahDitolakPadaTolakDanSelesai() throws Exception {
        JsonNode ditolak = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idTolak = ditolak.path("pemesanan").path("id_pemesanan").asText();

        mockMvc.perform(put("/api/pemesanan/" + idTolak + "/tolak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + ORANG_LAIN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pengguna tidak berwenang atas pemesanan ini"));

        JsonNode diselesaikan = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 1);
        String idSelesai = diselesaikan.path("pemesanan").path("id_pemesanan").asText();
        terima(idSelesai);

        mockMvc.perform(post("/api/pemesanan/" + idSelesai + "/selesai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + ORANG_LAIN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pengguna tidak berwenang atas pemesanan ini"));
    }

    @Test
    void waktuPesananFormatSalahDitolak() throws Exception {
        long jumlahAwal = pesananRepository.count();
        String isi = """
                {"id_pembeli":"%s","id_postingan":"%s","jumlah":1,
                 "waktu_pesanan":"31-12-2026 10:00"}
                """.formatted(PEMBELI, POSTINGAN_AKTIF);

        mockMvc.perform(post("/api/pemesanan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(isi))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.berhasil").value(false))
                .andExpect(jsonPath("$.pesan")
                        .value("Format waktu_pesanan tidak valid. Contoh: 2026-12-01T10:00:00"));

        assertThat(pesananRepository.count()).isEqualTo(jumlahAwal);
    }

    @Test
    void waktuPesananValidTersimpan() throws Exception {
        String isi = """
                {"id_pembeli":"%s","id_postingan":"%s","jumlah":1,
                 "waktu_pesanan":"2026-12-01 10:00:00"}
                """.formatted(PEMBELI, POSTINGAN_AKTIF);

        JsonNode data = kirimPost("/api/pemesanan", isi, 201);
        assertThat(data.path("pemesanan").path("waktu_pesanan").asText())
                .startsWith("2026-12-01T10:00");
    }

    @Test
    void menyelesaikanLayananSetelahPembayaranDikonfirmasi() throws Exception {
        JsonNode dibuat = buatPemesanan(PEMBELI, POSTINGAN_AKTIF, 2);
        String idPemesanan = dibuat.path("pemesanan").path("id_pemesanan").asText();
        String idPesanan = dibuat.path("pesanan").path("id_pesanan").asText();

        // belum diterima: tidak boleh selesai
        mockMvc.perform(post("/api/pemesanan/" + idPemesanan + "/selesai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isBadRequest());

        terima(idPemesanan);

        // sudah diterima tetapi belum ada pembayaran
        mockMvc.perform(post("/api/pemesanan/" + idPemesanan + "/selesai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pesan").value("Pembayaran tidak ditemukan"));

        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti-1.jpg");
        konfirmasiPembayaran(idPembayaran, PENYEDIA);

        mockMvc.perform(post("/api/pemesanan/" + idPemesanan + "/selesai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status_pemesanan").value("diterima"))
                .andExpect(jsonPath("$.data.status_pesanan").value("selesai"));

        Pesanan pesanan = pesananRepository.findById(idPesanan).orElseThrow();
        assertThat(pesanan.getStatusPesanan()).isEqualTo("selesai");
    }

    @Test
    void idPemesananTidakDitemukan() throws Exception {
        mockMvc.perform(get("/api/pemesanan/PMS999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pesan").value("Pemesanan tidak ditemukan"));

        mockMvc.perform(put("/api/pemesanan/PMS999/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/pemesanan/PMS999/selesai")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    private void simpanUser(String id, String username) {
        User user = new User();
        user.setIdUser(id);
        user.setUsername(username);
        user.setEmailAtauHp(username + "@contoh.test");
        user.setPassword("rahasia");
        user.setStatusAkun("aktif");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdateAt(LocalDateTime.now());
        user.setRataRataRating(0f);
        user.setBadgeKepercayaan("Pengguna Terpercaya Baru");
        userRepository.save(user);
    }

    private void simpanPostingan(String id, String idPenjual, String harga, boolean aktif) {
        jdbcTemplate.update(
                "INSERT INTO postingan (id_postingan, id_penjual, harga, status_aktif) VALUES (?, ?, ?, ?)",
                id, idPenjual, new BigDecimal(harga), aktif);
    }

    private String body(String idPembeli, String idPostingan, int jumlah) {
        return """
                {"id_pembeli":"%s","id_postingan":"%s","jumlah":%d}
                """.formatted(idPembeli, idPostingan, jumlah);
    }

    private JsonNode buatPemesanan(String idPembeli, String idPostingan, int jumlah) throws Exception {
        return kirimPost("/api/pemesanan", body(idPembeli, idPostingan, jumlah), 201);
    }

    private String buatPembayaran(String idPesanan) throws Exception {
        String isi = "{\"id_pesanan\":\"" + idPesanan + "\",\"metode\":\"transfer\"}";
        JsonNode data = kirimPost("/api/pembayaran", isi, 201);
        return data.path("pembayaran").path("id_pembayaran").asText();
    }

    private void terima(String idPemesanan) throws Exception {
        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk());
    }

    private void kirimBukti(String idPembayaran, String url) throws Exception {
        mockMvc.perform(post("/api/pembayaran/" + idPembayaran + "/bukti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bukti_pembayaran\":\"" + url + "\"}"))
                .andExpect(status().isCreated());
    }

    private void konfirmasiPembayaran(String idPembayaran, String idPenyedia) throws Exception {
        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + idPenyedia + "\"}"))
                .andExpect(status().isOk());
    }

    private JsonNode kirimPost(String url, String isi, int statusDiharapkan) throws Exception {
        MvcResult hasil = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(isi))
                .andExpect(status().is(statusDiharapkan))
                .andReturn();
        return objectMapper.readTree(hasil.getResponse().getContentAsString()).path("data");
    }
}
