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
class PembayaranControllerTest {

    private static final String PEMBELI = "USR001";
    private static final String PENYEDIA = "USR002";
    private static final String ORANG_LAIN = "USR003";
    private static final String POSTINGAN = "PST001";

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

    @Autowired
    private com.example.ubmager.repository.PemesananRepository pemesananRepository;

    @Autowired
    private com.example.ubmager.repository.PembayaranRepository pembayaranRepository;

    @BeforeEach
    void siapkanData() {
        simpanUser(PEMBELI, "pembeli_uji");
        simpanUser(PENYEDIA, "penyedia_uji");
        simpanUser(ORANG_LAIN, "orang_lain_uji");
        jdbcTemplate.update(
                "INSERT INTO postingan (id_postingan, id_penjual, harga, status_aktif) VALUES (?, ?, ?, ?)",
                POSTINGAN, PENYEDIA, new BigDecimal("50000.00"), true);
    }

    @Test
    void membuatPembayaranValid() throws Exception {
        String idPesanan = pesananDiterima(2);

        JsonNode data = kirimPost("/api/pembayaran",
                "{\"id_pesanan\":\"" + idPesanan + "\",\"metode\":\"transfer\"}", 201);

        assertThat(data.path("pembayaran").path("id_pembayaran").asText()).startsWith("PBY");
        assertThat(data.path("pembayaran").path("status").asText()).isEqualTo("menunggu_konfirmasi");
        assertThat(new BigDecimal(data.path("pembayaran").path("jumlah").asText()))
                .isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void pembayaranUntukPesananBelumDiterimaDitolak() throws Exception {
        String idPesanan = buatPemesanan(1);

        mockMvc.perform(post("/api/pembayaran")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_pesanan\":\"" + idPesanan + "\",\"metode\":\"transfer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pemesanan belum diterima"));
    }

    @Test
    void pembayaranGandaDitolak() throws Exception {
        String idPesanan = pesananDiterima(1);
        buatPembayaran(idPesanan);

        mockMvc.perform(post("/api/pembayaran")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_pesanan\":\"" + idPesanan + "\",\"metode\":\"tunai\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pembayaran sudah ada untuk pesanan ini"));
    }

    @Test
    void nominalMengikutiTotalHargaPesanan() throws Exception {
        String idPesanan = pesananDiterima(3);

        // jumlah pada request harus diabaikan
        JsonNode data = kirimPost("/api/pembayaran",
                "{\"id_pesanan\":\"" + idPesanan + "\",\"metode\":\"transfer\",\"jumlah\":1}", 201);

        assertThat(new BigDecimal(data.path("pembayaran").path("jumlah").asText()))
                .isEqualByComparingTo(new BigDecimal("150000.00"));
    }

    @Test
    void menyimpanBuktiPembayaran() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);

        mockMvc.perform(post("/api/pembayaran/" + idPembayaran + "/bukti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bukti_pembayaran\":\"https://contoh.test/bukti.jpg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id_data_pembayaran").value(org.hamcrest.Matchers.startsWith("DPY")))
                .andExpect(jsonPath("$.data.status").value("menunggu_konfirmasi"));

        mockMvc.perform(get("/api/pembayaran/" + idPembayaran))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riwayat_bukti.length()").value(1));
    }

    @Test
    void kirimBuktiUlangSetelahPerbaikan() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);

        kirimBukti(idPembayaran, "https://contoh.test/bukti-1.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/perbaikan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("perbaikan"));

        mockMvc.perform(post("/api/pembayaran/" + idPembayaran + "/bukti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bukti_pembayaran\":\"https://contoh.test/bukti-2.jpg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("menunggu_konfirmasi"));

        mockMvc.perform(get("/api/pembayaran/" + idPembayaran))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pembayaran.status").value("menunggu_konfirmasi"))
                .andExpect(jsonPath("$.data.riwayat_bukti.length()").value(2));
    }

    @Test
    void konfirmasiMengubahStatusPembayaranDanPesanan() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("dikonfirmasi"))
                .andExpect(jsonPath("$.data.status_pesanan").value("dibayar"));

        Pesanan pesanan = pesananRepository.findById(idPesanan).orElseThrow();
        assertThat(pesanan.getStatusPesanan()).isEqualTo("dibayar");
    }

    @Test
    void memintaPerbaikanTidakMengubahStatusPesanan() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/perbaikan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("perbaikan"))
                .andExpect(jsonPath("$.data.status_pesanan").value("menunggu"));

        Pesanan pesanan = pesananRepository.findById(idPesanan).orElseThrow();
        assertThat(pesanan.getStatusPesanan()).isEqualTo("menunggu");
    }

    @Test
    void konfirmasiOlehPenyediaTidakSesuaiDitolak() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + ORANG_LAIN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pengguna tidak berwenang atas pembayaran ini"));
    }

    @Test
    void konfirmasiGandaDitolakDanBuktiSetelahKonfirmasiDitolak() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");
        konfirmasi(idPembayaran, PENYEDIA);

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pembayaran sudah dikonfirmasi"));

        mockMvc.perform(post("/api/pembayaran/" + idPembayaran + "/bukti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bukti_pembayaran\":\"https://contoh.test/lain.jpg\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void konfirmasiTanpaIdPenyediaDitolak() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));

        assertThat(pembayaranRepository.findById(idPembayaran).orElseThrow().getStatus())
                .isEqualTo("menunggu_konfirmasi");
        assertThat(pesananRepository.findById(idPesanan).orElseThrow().getStatusPesanan())
                .isEqualTo("menunggu");
    }

    @Test
    void perbaikanTanpaIdPenyediaDitolak() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/perbaikan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("ID penyedia wajib dikirim"));

        assertThat(pembayaranRepository.findById(idPembayaran).orElseThrow().getStatus())
                .isEqualTo("menunggu_konfirmasi");
    }

    @Test
    void perbaikanOlehPenyediaTidakSesuaiDitolak() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);
        kirimBukti(idPembayaran, "https://contoh.test/bukti.jpg");

        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/perbaikan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + ORANG_LAIN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.pesan").value("Pengguna tidak berwenang atas pembayaran ini"));

        assertThat(pembayaranRepository.findById(idPembayaran).orElseThrow().getStatus())
                .isEqualTo("menunggu_konfirmasi");
    }

    @Test
    void melihatPembayaranBerdasarkanId() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);

        mockMvc.perform(get("/api/pembayaran/" + idPembayaran))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pembayaran.id_pembayaran").value(idPembayaran))
                .andExpect(jsonPath("$.data.pesanan.id_pesanan").value(idPesanan));
    }

    @Test
    void melihatPembayaranBerdasarkanPesanan() throws Exception {
        String idPesanan = pesananDiterima(1);
        String idPembayaran = buatPembayaran(idPesanan);

        mockMvc.perform(get("/api/pembayaran/pesanan/" + idPesanan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pembayaran.id_pembayaran").value(idPembayaran));
    }

    @Test
    void idTidakDitemukan() throws Exception {
        mockMvc.perform(get("/api/pembayaran/PBY999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pesan").value("Pembayaran tidak ditemukan"));

        mockMvc.perform(get("/api/pembayaran/pesanan/PSN999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/pembayaran")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_pesanan\":\"PSN999\",\"metode\":\"transfer\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.pesan").value("Pesanan tidak ditemukan"));

        mockMvc.perform(put("/api/pembayaran/PBY999/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    private String pesananDiterima(int jumlah) throws Exception {
        String idPesanan = buatPemesanan(jumlah);
        String idPemesanan = pemesananRepositoryId(idPesanan);
        mockMvc.perform(put("/api/pemesanan/" + idPemesanan + "/terima")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + PENYEDIA + "\"}"))
                .andExpect(status().isOk());
        return idPesanan;
    }

    private String buatPemesanan(int jumlah) throws Exception {
        String isi = """
                {"id_pembeli":"%s","id_postingan":"%s","jumlah":%d}
                """.formatted(PEMBELI, POSTINGAN, jumlah);
        JsonNode data = kirimPost("/api/pemesanan", isi, 201);
        return data.path("pesanan").path("id_pesanan").asText();
    }

    private String pemesananRepositoryId(String idPesanan) {
        return pemesananRepository.findByIdPesanan(idPesanan).orElseThrow().getIdPemesanan();
    }

    private String buatPembayaran(String idPesanan) throws Exception {
        JsonNode data = kirimPost("/api/pembayaran",
                "{\"id_pesanan\":\"" + idPesanan + "\",\"metode\":\"transfer\"}", 201);
        return data.path("pembayaran").path("id_pembayaran").asText();
    }

    private void kirimBukti(String idPembayaran, String url) throws Exception {
        mockMvc.perform(post("/api/pembayaran/" + idPembayaran + "/bukti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bukti_pembayaran\":\"" + url + "\"}"))
                .andExpect(status().isCreated());
    }

    private void konfirmasi(String idPembayaran, String idPenyedia) throws Exception {
        mockMvc.perform(put("/api/pembayaran/" + idPembayaran + "/konfirmasi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id_penyedia\":\"" + idPenyedia + "\"}"))
                .andExpect(status().isOk());
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

    private JsonNode kirimPost(String url, String isi, int statusDiharapkan) throws Exception {
        MvcResult hasil = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(isi))
                .andExpect(status().is(statusDiharapkan))
                .andReturn();
        return objectMapper.readTree(hasil.getResponse().getContentAsString()).path("data");
    }
}
