package com.example.ubmager.service;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Keranjang;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.entity.ProdukKeranjang;
import com.example.ubmager.entity.Transaksi;
import com.example.ubmager.repository.KeranjangRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.ProdukKeranjangRepository;
import com.example.ubmager.repository.TransaksiRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.IdGenerator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pencatat transaksi (SR-49, SR-50, SR-51).
 *
 * Service ini hanya MENULIS ke tabel transaksi. Halaman dan API riwayat
 * (RiwayatController) tetap hanya membaca tabel yang sama.
 *
 * Cara memakai dari fitur lain (pemesanan / pembayaran):
 *   1. Inject: private final TransaksiService transaksiService;
 *   2. Panggil di momen yang disepakati, misalnya saat pesanan dibayar:
 *        Hasil hasil = transaksiService.catatDariPesanan(idPesanan, 1);
 *      atau saat pembayaran dikonfirmasi dan semua produk terpilih di keranjang menjadi transaksi:
 *        Hasil hasil = transaksiService.catatDariKeranjang(idUser, "dibayar");
 *   3. Saat status berubah (dibayar, selesai, dibatalkan):
 *        transaksiService.ubahStatus(idTransaksi, "selesai");
 */
@Service
public class TransaksiService {

    public static final List<String> STATUS_VALID = List.of("menunggu", "dibayar", "selesai", "dibatalkan");

    private final TransaksiRepository transaksiRepository;
    private final PostinganRepository postinganRepository;
    private final PesananRepository pesananRepository;
    private final UserRepository userRepository;
    private final KeranjangRepository keranjangRepository;
    private final ProdukKeranjangRepository produkKeranjangRepository;
    private final IdGenerator idGenerator;

    public TransaksiService(TransaksiRepository transaksiRepository,
            PostinganRepository postinganRepository,
            PesananRepository pesananRepository,
            UserRepository userRepository,
            KeranjangRepository keranjangRepository,
            ProdukKeranjangRepository produkKeranjangRepository,
            IdGenerator idGenerator) {
        this.transaksiRepository = transaksiRepository;
        this.postinganRepository = postinganRepository;
        this.pesananRepository = pesananRepository;
        this.userRepository = userRepository;
        this.keranjangRepository = keranjangRepository;
        this.produkKeranjangRepository = produkKeranjangRepository;
        this.idGenerator = idGenerator;
    }

    // Mencatat satu transaksi. Judul dan kategori diambil dari postingan,
    // total harga = harga postingan x jumlah.
    @Transactional
    public Hasil catatTransaksi(String idUser, String idPostingan, int jumlah, String status) {
        if (!userRepository.existsById(idUser)) {
            return new Hasil(false, "Pengguna tidak ditemukan", null);
        }
        Optional<Postingan> post = postinganRepository.findById(idPostingan);
        if (post.isEmpty()) {
            return new Hasil(false, "Postingan tidak ditemukan", null);
        }
        double total = post.get().getHarga().doubleValue() * jumlah;
        return simpan(idUser, post.get(), jumlah, total, status);
    }

    // Mencatat transaksi dari satu pesanan (pembeli dan postingan diambil dari pesanan).
    // Status transaksi mengikuti status pesanan bila termasuk status yang valid,
    // selain itu dicatat sebagai "menunggu".
    @Transactional
    public Hasil catatDariPesanan(String idPesanan, int jumlah) {
        Optional<Pesanan> pesanan = pesananRepository.findById(idPesanan);
        if (pesanan.isEmpty()) {
            return new Hasil(false, "Pesanan tidak ditemukan", null);
        }
        String status = pesanan.get().getStatusPesanan() == null
                ? "menunggu"
                : pesanan.get().getStatusPesanan().toLowerCase();
        if (!STATUS_VALID.contains(status)) {
            status = "menunggu";
        }
        return catatTransaksi(pesanan.get().getIdPembeli(), pesanan.get().getIdPostingan(), jumlah, status);
    }

    // Mencatat seluruh produk yang dicentang di keranjang sebagai transaksi
    // (satu baris per produk), lalu mengeluarkannya dari keranjang.
    // Harga memakai harga satuan yang tersimpan di keranjang.
    @Transactional
    public Hasil catatDariKeranjang(String idUser, String status) {
        if (!userRepository.existsById(idUser)) {
            return new Hasil(false, "Pengguna tidak ditemukan", null);
        }
        Optional<Keranjang> keranjang = keranjangRepository.findByIdUser(idUser);
        if (keranjang.isEmpty()) {
            return new Hasil(false, "Keranjang tidak ditemukan", null);
        }
        List<ProdukKeranjang> terpilih = new ArrayList<>();
        for (ProdukKeranjang item : produkKeranjangRepository
                .findByIdKeranjangAndDipilihTrue(keranjang.get().getIdKeranjang())) {
            if (!item.isTidakTersedia()) {
                terpilih.add(item);
            }
        }
        if (terpilih.isEmpty()) {
            return new Hasil(false, "Tidak ada produk terpilih di keranjang", null);
        }

        List<Transaksi> dicatat = new ArrayList<>();
        for (ProdukKeranjang item : terpilih) {
            Optional<Postingan> post = postinganRepository.findById(item.getIdPostingan());
            if (post.isEmpty()) {
                return new Hasil(false, "Postingan tidak ditemukan", null);
            }
            Hasil hasil = simpan(idUser, post.get(), item.getJumlah(),
                    item.getHargaSatuan() * item.getJumlah(), status);
            if (!hasil.berhasil()) {
                throw new IllegalStateException(hasil.pesan());
            }
            dicatat.add((Transaksi) hasil.data());
        }
        produkKeranjangRepository.deleteAll(terpilih);

        keranjang.get().setUpdatedAt(LocalDateTime.now());
        keranjangRepository.save(keranjang.get());
        return new Hasil(true, "Transaksi berhasil dicatat", dicatat);
    }

    // Mengubah status transaksi (menunggu, dibayar, selesai, dibatalkan).
    @Transactional
    public Hasil ubahStatus(String idTransaksi, String statusBaru) {
        Optional<Transaksi> transaksi = transaksiRepository.findById(idTransaksi);
        if (transaksi.isEmpty()) {
            return new Hasil(false, "Transaksi tidak ditemukan", null);
        }
        String status = statusBaru == null ? "" : statusBaru.toLowerCase();
        if (!STATUS_VALID.contains(status)) {
            return new Hasil(false, "Status transaksi tidak valid", null);
        }
        transaksi.get().setStatusTransaksi(status);
        transaksiRepository.save(transaksi.get());
        return new Hasil(true, "Status transaksi diperbarui", transaksi.get());
    }

    // Menyusun dan menyimpan satu baris transaksi
    private Hasil simpan(String idUser, Postingan post, int jumlah, double totalHarga, String status) {
        if (jumlah < 1) {
            return new Hasil(false, "Jumlah transaksi tidak valid", null);
        }
        String statusBersih = status == null ? "" : status.toLowerCase();
        if (!STATUS_VALID.contains(statusBersih)) {
            return new Hasil(false, "Status transaksi tidak valid", null);
        }

        Transaksi transaksi = new Transaksi();
        transaksi.setIdTransaksi(idGenerator.buat("TRX", "transaksi", "id_transaksi"));
        transaksi.setIdUser(idUser);
        transaksi.setIdPostingan(post.getIdPostingan());
        transaksi.setJudulPostingan(post.getJudul());
        transaksi.setKategori(post.getKategori());
        transaksi.setJumlah(jumlah);
        transaksi.setTotalHarga(totalHarga);
        transaksi.setTanggalTransaksi(LocalDateTime.now());
        transaksi.setStatusTransaksi(statusBersih);
        // saveAndFlush agar ID berikutnya (TRX002, TRX003, ...) terhitung benar saat mencatat banyak baris
        transaksiRepository.saveAndFlush(transaksi);
        return new Hasil(true, "Transaksi berhasil dicatat", transaksi);
    }
}
