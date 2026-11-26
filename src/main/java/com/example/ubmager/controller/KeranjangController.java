package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Keranjang;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.entity.ProdukKeranjang;
import com.example.ubmager.repository.KeranjangRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.ProdukKeranjangRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/keranjang")
@Transactional
public class KeranjangController {

    private final UserRepository userRepository;
    private final KeranjangRepository keranjangRepository;
    private final ProdukKeranjangRepository produkKeranjangRepository;
    private final PostinganRepository postinganRepository;
    private final IdGenerator idGenerator;

    public KeranjangController(UserRepository userRepository,
            KeranjangRepository keranjangRepository,
            ProdukKeranjangRepository produkKeranjangRepository,
            PostinganRepository postinganRepository,
            IdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.keranjangRepository = keranjangRepository;
        this.produkKeranjangRepository = produkKeranjangRepository;
        this.postinganRepository = postinganRepository;
        this.idGenerator = idGenerator;
    }

    // Buka keranjang
    @GetMapping("/{idUser}")
    public ResponseEntity<Hasil> buka(@PathVariable String idUser) {
        return prosesKeranjang("BUKA_KERANJANG", idUser, Map.of()).kirim(HttpStatus.OK);
    }

    // Simpan produk ke keranjang
    @PostMapping("/{idUser}/produk")
    public ResponseEntity<Hasil> simpan(@PathVariable String idUser,
            @RequestBody Map<String, Object> body) {
        return prosesKeranjang("SIMPAN_PRODUK", idUser, body).kirim(HttpStatus.CREATED);
    }

    // Ubah detail pesanan pada produk di keranjang
    @PutMapping("/{idUser}/produk/{idProduk}")
    public ResponseEntity<Hasil> ubah(@PathVariable String idUser,
            @PathVariable String idProduk, @RequestBody Map<String, Object> body) {
        body.put("id_produk", idProduk);
        return prosesKeranjang("UBAH_DETAIL", idUser, body).kirim(HttpStatus.OK);
    }

    // Hapus satu atau beberapa produk dari keranjang
    @DeleteMapping("/{idUser}/produk")
    public ResponseEntity<Hasil> hapus(@PathVariable String idUser,
            @RequestBody Map<String, Object> body) {
        return prosesKeranjang("HAPUS_PRODUK", idUser, body).kirim(HttpStatus.OK);
    }

    // Isi atau kosongkan checkbox satu produk
    @PutMapping("/{idUser}/produk/{idProduk}/pilih")
    public ResponseEntity<Hasil> pilih(@PathVariable String idUser,
            @PathVariable String idProduk, @RequestBody Map<String, Object> body) {
        body.put("id_produk", idProduk);
        return prosesKeranjang("PILIH_CHECKBOX", idUser, body).kirim(HttpStatus.OK);
    }

    // Pilih semua produk yang tersedia
    @PutMapping("/{idUser}/pilih-semua")
    public ResponseEntity<Hasil> pilihSemua(@PathVariable String idUser) {
        return prosesKeranjang("PILIH_SEMUA", idUser, Map.of()).kirim(HttpStatus.OK);
    }

    // Checkout produk yang dipilih
    @PostMapping("/{idUser}/checkout")
    public ResponseEntity<Hasil> checkout(@PathVariable String idUser) {
        return prosesKeranjang("CHECKOUT", idUser, Map.of()).kirim(HttpStatus.OK);
    }

    // Method untuk memproses seluruh aksi pada keranjang pemesanan
    public Hasil prosesKeranjang(String aksi, String idUser, Map<String, Object> data) {
        // Pengguna harus terdaftar dan login
        if (!userRepository.existsById(idUser)) {
            return new Hasil(false, "Silakan login terlebih dahulu", null);
        }

        Keranjang keranjang = ambilAtauBuatKeranjang(idUser);
        String idKeranjang = keranjang.getIdKeranjang();

        if (aksi.equals("BUKA_KERANJANG")) {
            // Hapus produk yang sudah ditandai tidak tersedia sebelumnya
            produkKeranjangRepository.deleteByIdKeranjangAndTidakTersediaTrue(idKeranjang);

            // Periksa produk lain yang baru saja tidak tersedia
            List<ProdukKeranjang> peringatan = new ArrayList<>();
            for (ProdukKeranjang item : produkKeranjangRepository.findByIdKeranjang(idKeranjang)) {
                if (!postinganAktif(item.getIdPostingan())) {
                    tandaiTidakTersedia(item);
                    peringatan.add(item);
                }
            }
            return new Hasil(true, "Keranjang ditemukan", ringkasan(idKeranjang, peringatan));

        } else if (aksi.equals("SIMPAN_PRODUK")) {
            Optional<Postingan> post = postinganRepository.findById(teks(data, "id_postingan"));
            if (post.isEmpty() || !aktif(post.get())) {
                return new Hasil(false, "Produk sudah tidak tersedia", null);
            }
            int jumlah = angka(data, "jumlah", 1);
            if (jumlah < 1) {
                return new Hasil(false, "Jumlah pesanan tidak valid", null);
            }

            ProdukKeranjang item = new ProdukKeranjang();
            item.setIdProduk(idGenerator.buat("PRD", "produk_keranjang", "id_produk"));
            item.setIdKeranjang(idKeranjang);
            item.setIdPostingan(post.get().getIdPostingan());
            item.setJumlah(jumlah);
            isiDetail(item, data);
            item.setHargaSatuan(post.get().getHarga().doubleValue());
            item.setDipilih(false);
            item.setTidakTersedia(false);
            produkKeranjangRepository.save(item);
            perbaruiWaktu(keranjang);
            return new Hasil(true, "Produk berhasil disimpan ke keranjang", item);

        } else if (aksi.equals("UBAH_DETAIL")) {
            ProdukKeranjang item = cariItem(idKeranjang, teks(data, "id_produk"));
            if (item == null) {
                return new Hasil(false, "Item keranjang tidak ditemukan", null);
            }
            if (item.isTidakTersedia() || !postinganAktif(item.getIdPostingan())) {
                tandaiTidakTersedia(item);
                return new Hasil(false, "Peringatan: Produk tidak tersedia, detail tidak dapat diubah",
                        ringkasan(idKeranjang, List.of(item)));
            }
            if (data.containsKey("jumlah")) {
                int jumlah = angka(data, "jumlah", item.getJumlah());
                if (jumlah < 1) {
                    return new Hasil(false, "Jumlah pesanan tidak valid", null);
                }
                item.setJumlah(jumlah);
            }
            isiDetail(item, data);
            produkKeranjangRepository.save(item);
            perbaruiWaktu(keranjang);
            return new Hasil(true, "Detail pesanan berhasil diubah", ringkasan(idKeranjang, List.of()));

        } else if (aksi.equals("HAPUS_PRODUK")) {
            Object mentah = data.get("list_id_produk");
            if (!(mentah instanceof List<?> daftarId) || daftarId.isEmpty()) {
                return new Hasil(false, "Item keranjang tidak ditemukan", null);
            }
            List<ProdukKeranjang> dihapus = new ArrayList<>();
            for (Object id : daftarId) {
                ProdukKeranjang item = cariItem(idKeranjang, String.valueOf(id));
                if (item == null) {
                    return new Hasil(false, "Item keranjang tidak ditemukan", null);
                }
                dihapus.add(item);
            }
            produkKeranjangRepository.deleteAll(dihapus);
            perbaruiWaktu(keranjang);
            return new Hasil(true, "Produk berhasil dihapus dari keranjang",
                    ringkasan(idKeranjang, List.of()));

        } else if (aksi.equals("PILIH_CHECKBOX")) {
            ProdukKeranjang item = cariItem(idKeranjang, teks(data, "id_produk"));
            if (item == null) {
                return new Hasil(false, "Item keranjang tidak ditemukan", null);
            }
            if (item.isTidakTersedia() || !postinganAktif(item.getIdPostingan())) {
                tandaiTidakTersedia(item);
                return new Hasil(false, "Peringatan: Produk tidak tersedia dan tidak dapat dipilih",
                        ringkasan(idKeranjang, List.of(item)));
            }
            item.setDipilih(Boolean.parseBoolean(teks(data, "dipilih")));
            produkKeranjangRepository.save(item);
            return new Hasil(true, "Status checkbox diperbarui", ringkasan(idKeranjang, List.of()));

        } else if (aksi.equals("PILIH_SEMUA")) {
            for (ProdukKeranjang item : produkKeranjangRepository.findByIdKeranjang(idKeranjang)) {
                if (!item.isTidakTersedia()) {
                    item.setDipilih(true);
                    produkKeranjangRepository.save(item);
                }
            }
            return new Hasil(true, "Semua produk tersedia dipilih", ringkasan(idKeranjang, List.of()));

        } else if (aksi.equals("CHECKOUT")) {
            List<ProdukKeranjang> terpilih =
                    produkKeranjangRepository.findByIdKeranjangAndDipilihTrue(idKeranjang);
            if (terpilih.isEmpty()) {
                return new Hasil(false, "Pilih minimal satu produk untuk checkout", null);
            }

            // Periksa ulang ketersediaan produk yang dipilih
            List<ProdukKeranjang> peringatan = new ArrayList<>();
            for (ProdukKeranjang item : terpilih) {
                if (!postinganAktif(item.getIdPostingan())) {
                    tandaiTidakTersedia(item);
                    peringatan.add(item);
                }
            }
            if (!peringatan.isEmpty()) {
                return new Hasil(false, "Peringatan: Produk tidak tersedia",
                        ringkasan(idKeranjang, peringatan));
            }
            return new Hasil(true, "Checkout dapat dilanjutkan ke proses pembayaran",
                    ringkasan(idKeranjang, peringatan));
        }
        return new Hasil(false, "Aksi tidak valid", null);
    }

    // Method untuk menghitung total biaya dan total porsi produk yang dipilih dan tersedia
    public double[] hitungTotal(String idKeranjang) {
        double totalBiaya = 0;
        double totalPorsi = 0;
        for (ProdukKeranjang item : produkKeranjangRepository.findByIdKeranjangAndDipilihTrue(idKeranjang)) {
            if (!item.isTidakTersedia()) {
                totalBiaya += item.getHargaSatuan() * item.getJumlah();
                totalPorsi += item.getJumlah();
            }
        }
        return new double[] { totalBiaya, totalPorsi };
    }

    // Method untuk mengambil keranjang milik pengguna, dibuat baru jika belum ada
    private Keranjang ambilAtauBuatKeranjang(String idUser) {
        return keranjangRepository.findByIdUser(idUser).orElseGet(() -> {
            Keranjang baru = new Keranjang();
            baru.setIdKeranjang(idGenerator.buat("KRG", "keranjang", "id_keranjang"));
            baru.setIdUser(idUser);
            baru.setCreatedAt(LocalDateTime.now());
            baru.setUpdatedAt(LocalDateTime.now());
            return keranjangRepository.save(baru);
        });
    }

    // Method untuk mencari item yang benar-benar milik keranjang pengguna
    private ProdukKeranjang cariItem(String idKeranjang, String idProduk) {
        return produkKeranjangRepository.findById(idProduk)
                .filter(item -> item.getIdKeranjang().equals(idKeranjang))
                .orElse(null);
    }

    // Method untuk menandai produk tidak tersedia dan melepas checkbox-nya
    private void tandaiTidakTersedia(ProdukKeranjang item) {
        item.setTidakTersedia(true);
        item.setDipilih(false);
        produkKeranjangRepository.save(item);
    }

    // Method untuk mengisi jadwal, lokasi, dan catatan dari body jika ada
    private void isiDetail(ProdukKeranjang item, Map<String, Object> data) {
        if (data.containsKey("jadwal") && !teks(data, "jadwal").isBlank()) {
            item.setJadwal(LocalDateTime.parse(teks(data, "jadwal")));
        }
        if (data.containsKey("lokasi")) {
            item.setLokasi(teks(data, "lokasi"));
        }
        if (data.containsKey("catatan")) {
            item.setCatatan(teks(data, "catatan"));
        }
    }

    // Method untuk menyusun daftar item, total biaya, total porsi, dan peringatan
    private Map<String, Object> ringkasan(String idKeranjang, List<ProdukKeranjang> peringatan) {
        double[] total = hitungTotal(idKeranjang);
        Map<String, Object> hasil = new LinkedHashMap<>();
        hasil.put("daftar_item", produkKeranjangRepository.findByIdKeranjang(idKeranjang));
        hasil.put("total_biaya", total[0]);
        hasil.put("total_porsi", (int) total[1]);
        hasil.put("daftar_peringatan", peringatan);
        return hasil;
    }

    private void perbaruiWaktu(Keranjang keranjang) {
        keranjang.setUpdatedAt(LocalDateTime.now());
        keranjangRepository.save(keranjang);
    }

    // Ketersediaan produk: postingan berstatus aktif dan belum melewati jam tayang
    private boolean postinganAktif(String idPostingan) {
        return postinganRepository.findById(idPostingan).map(this::aktif).orElse(false);
    }

    private boolean aktif(Postingan post) {
        return post.isStatusAktif()
            && post.getJamTayang().isAfter(LocalDateTime.now());
    }

    private int angka(Map<String, Object> data, String kunci, int bawaan) {
        Object nilai = data.get(kunci);
        return nilai == null ? bawaan : Integer.parseInt(nilai.toString());
    }

    private String teks(Map<String, Object> data, String kunci) {
        Object nilai = data.get(kunci);
        return nilai == null ? "" : nilai.toString();
    }
}