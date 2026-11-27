package com.example.ubmager.controller;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Keranjang;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.entity.ProdukKeranjang;
import com.example.ubmager.repository.KeranjangRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.ProdukKeranjangRepository;
import jakarta.servlet.http.HttpSession;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/keranjang")
public class HalamanKeranjangController {

    public record ItemKeranjangView(String idProduk, String judul, String kategori, String inisial,
            int jumlah, String hargaTampil, String subtotalTampil,
            boolean dipilih, boolean tidakTersedia,
            String jadwalTampil, String jadwalInput,
            String lokasi, String catatan) {
    }

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter TAMPIL = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", ID);
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final KeranjangController keranjangController;
    private final KeranjangRepository keranjangRepository;
    private final ProdukKeranjangRepository produkKeranjangRepository;
    private final PostinganRepository postinganRepository;

    public HalamanKeranjangController(KeranjangController keranjangController,
            KeranjangRepository keranjangRepository,
            ProdukKeranjangRepository produkKeranjangRepository,
            PostinganRepository postinganRepository) {
        this.keranjangController = keranjangController;
        this.keranjangRepository = keranjangRepository;
        this.produkKeranjangRepository = produkKeranjangRepository;
        this.postinganRepository = postinganRepository;
    }

    // Halaman keranjang. Tanpa parameter "lanjut" (dibuka dari menu), produk yang
    // sudah
    // ditandai tidak tersedia dihapus dan ketersediaan seluruh produk diperiksa
    // ulang.
    @GetMapping
    public String lihat(@RequestParam(required = false) String lanjut, HttpSession session, Model model) {
        String idUser = idUser(session);
        if (idUser == null) {
            return "redirect:/login";
        }
        if (lanjut == null) {
            keranjangController.prosesKeranjang("BUKA_KERANJANG", idUser, Map.of());
        }

        List<ItemKeranjangView> items = new ArrayList<>();
        List<String> peringatan = new ArrayList<>();
        int jumlahDipilih = 0;
        int jumlahTersedia = 0;
        double totalBiaya = 0;
        int totalPorsi = 0;

        Keranjang keranjang = keranjangRepository.findByIdUser(idUser).orElse(null);
        if (keranjang != null) {
            List<ProdukKeranjang> daftar = produkKeranjangRepository.findByIdKeranjang(keranjang.getIdKeranjang());
            daftar.sort(Comparator.comparing(ProdukKeranjang::getIdProduk));

            for (ProdukKeranjang p : daftar) {
                Postingan post = postinganRepository.findById(p.getIdPostingan()).orElse(null);
                String judul = post == null ? "Produk tidak ditemukan" : post.getJudul();
                String kategori = post == null ? "-" : post.getKategori();

                items.add(new ItemKeranjangView(
                        p.getIdProduk(), judul, kategori,
                        judul.isBlank() ? "?" : judul.substring(0, 1).toUpperCase(),
                        p.getJumlah(), rupiah(p.getHargaSatuan()), rupiah(p.getHargaSatuan() * p.getJumlah()),
                        p.isDipilih(), p.isTidakTersedia(),
                        p.getJadwal() == null ? "" : p.getJadwal().format(TAMPIL),
                        p.getJadwal() == null ? "" : p.getJadwal().format(INPUT),
                        p.getLokasi() == null ? "" : p.getLokasi(),
                        p.getCatatan() == null ? "" : p.getCatatan()));

                if (p.isTidakTersedia()) {
                    peringatan.add(judul);
                } else {
                    jumlahTersedia++;
                    if (p.isDipilih()) {
                        jumlahDipilih++;
                    }
                }
            }
            double[] hitung = keranjangController.hitungTotal(keranjang.getIdKeranjang());
            totalBiaya = hitung[0];
            totalPorsi = (int) hitung[1];
        }

        model.addAttribute("items", items);
        model.addAttribute("peringatan", peringatan);
        model.addAttribute("jumlahDipilih", jumlahDipilih);
        model.addAttribute("adaTersedia", jumlahTersedia > 0);
        model.addAttribute("semuaTerpilih", jumlahTersedia > 0 && jumlahDipilih == jumlahTersedia);
        model.addAttribute("totalPorsi", totalPorsi);
        model.addAttribute("totalTampil", rupiah(totalBiaya));
        return "keranjang/index";
    }

    @PostMapping("/pilih")
    public String pilih(@RequestParam String idProduk, @RequestParam boolean dipilih,
            HttpSession session, RedirectAttributes ra) {
        return jalankan("PILIH_CHECKBOX",
                Map.of("id_produk", idProduk, "dipilih", String.valueOf(dipilih)), session, ra, false);
    }

    @PostMapping("/pilih-semua")
    public String pilihSemua(@RequestParam boolean semua, HttpSession session, RedirectAttributes ra) {
        String idUser = idUser(session);
        if (idUser == null) {
            return "redirect:/login";
        }
        if (semua) {
            return jalankan("PILIH_SEMUA", Map.of(), session, ra, false);
        }
        // Kosongkan seluruh centang pada produk yang tersedia
        keranjangRepository.findByIdUser(idUser).ifPresent(k -> {
            for (ProdukKeranjang p : produkKeranjangRepository.findByIdKeranjang(k.getIdKeranjang())) {
                if (!p.isTidakTersedia()) {
                    keranjangController.prosesKeranjang("PILIH_CHECKBOX", idUser,
                            Map.of("id_produk", p.getIdProduk(), "dipilih", "false"));
                }
            }
        });
        return "redirect:/keranjang?lanjut=1";
    }

    // Stepper jumlah (tombol minus dan plus)
    @PostMapping("/jumlah")
    public String jumlah(@RequestParam String idProduk, @RequestParam int jumlah,
            HttpSession session, RedirectAttributes ra) {
        if (jumlah < 1 || jumlah > 99) {
            return "redirect:/keranjang?lanjut=1";
        }
        return jalankan("UBAH_DETAIL", Map.of("id_produk", idProduk, "jumlah", jumlah), session, ra, false);
    }

    // Ubah detail: jadwal, lokasi, catatan (jumlah diubah lewat stepper)
    @PostMapping("/ubah")
    public String ubah(@RequestParam String idProduk,
            @RequestParam(defaultValue = "") String jadwal,
            @RequestParam(defaultValue = "") String lokasi,
            @RequestParam(defaultValue = "") String catatan,
            HttpSession session, RedirectAttributes ra) {
        Map<String, Object> data = new HashMap<>();
        data.put("id_produk", idProduk);
        data.put("jadwal", jadwal);
        data.put("lokasi", lokasi);
        data.put("catatan", catatan);
        return jalankan("UBAH_DETAIL", data, session, ra, true);
    }

    @PostMapping("/hapus")
    public String hapus(@RequestParam String idProduk, HttpSession session, RedirectAttributes ra) {
        return jalankan("HAPUS_PRODUK", Map.of("list_id_produk", List.of(idProduk)), session, ra, true);
    }

    // Dipanggil dari tombol "Simpan ke keranjang" di halaman postingan (milik fitur
    // lain)
    @PostMapping("/tambah")
    public String tambah(@RequestParam String idPostingan,
            @RequestParam(defaultValue = "1") int jumlah,
            HttpSession session, RedirectAttributes ra) {
        return jalankan("SIMPAN_PRODUK",
                Map.of("id_postingan", idPostingan, "jumlah", jumlah), session, ra, true);
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session, RedirectAttributes ra) {
        // TODO: setelah halaman pembayaran (fitur MI) siap, arahkan redirect ke sana
        // saat berhasil
        return jalankan("CHECKOUT", Map.of(), session, ra, true);
    }

    // Menjalankan aksi di KeranjangController lalu kembali ke halaman keranjang
    private String jalankan(String aksi, Map<String, Object> data, HttpSession session,
            RedirectAttributes ra, boolean tampilkanPesanSukses) {
        String idUser = idUser(session);
        if (idUser == null) {
            return "redirect:/login";
        }
        Hasil hasil = keranjangController.prosesKeranjang(aksi, idUser, data);
        if (!hasil.berhasil()) {
            ra.addFlashAttribute("pesanError", hasil.pesan());
        } else if (tampilkanPesanSukses) {
            ra.addFlashAttribute("pesanSukses", hasil.pesan());
        }
        return "redirect:/keranjang?lanjut=1";
    }

    private String idUser(HttpSession session) {
        Object id = session.getAttribute("idUser");
        return id == null ? null : id.toString();
    }

    private String rupiah(double nilai) {
        NumberFormat format = NumberFormat.getInstance(ID);
        format.setMaximumFractionDigits(0);
        return "Rp " + format.format(nilai);
    }
}