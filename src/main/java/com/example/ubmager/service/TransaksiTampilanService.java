package com.example.ubmager.service;

import com.example.ubmager.dto.BuktiTampil;
import com.example.ubmager.dto.JasaTampil;
import com.example.ubmager.dto.PembayaranTampil;
import com.example.ubmager.dto.PemesananTampil;
import com.example.ubmager.entity.DataPembayaran;
import com.example.ubmager.entity.Pembayaran;
import com.example.ubmager.entity.Pemesanan;
import com.example.ubmager.entity.Pesanan;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.entity.Profil;
import com.example.ubmager.entity.User;
import com.example.ubmager.repository.DataPembayaranRepository;
import com.example.ubmager.repository.PembayaranRepository;
import com.example.ubmager.repository.PemesananRepository;
import com.example.ubmager.repository.PesananRepository;
import com.example.ubmager.repository.PostinganBacaRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.repository.ProfilRepository;
import com.example.ubmager.repository.UserRepository;
import com.example.ubmager.util.Teks;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Menyusun data halaman Pemesanan dan Pembayaran dari repository yang sudah ada.
 * Tidak membuat endpoint baru: seluruh aksi tulis tetap lewat REST API
 * PemesananController dan PembayaranController.
 */
@Service
public class TransaksiTampilanService {

    private static final Locale ID = Locale.forLanguageTag("id-ID");
    private static final DateTimeFormatter WAKTU = DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", ID);

    private final UserRepository userRepository;
    private final ProfilRepository profilRepository;
    private final PostinganRepository postinganRepository;
    private final PostinganBacaRepository postinganBacaRepository;
    private final PesananRepository pesananRepository;
    private final PemesananRepository pemesananRepository;
    private final PembayaranRepository pembayaranRepository;
    private final DataPembayaranRepository dataPembayaranRepository;

    public TransaksiTampilanService(UserRepository userRepository, ProfilRepository profilRepository,
                                    PostinganRepository postinganRepository,
                                    PostinganBacaRepository postinganBacaRepository,
                                    PesananRepository pesananRepository,
                                    PemesananRepository pemesananRepository,
                                    PembayaranRepository pembayaranRepository,
                                    DataPembayaranRepository dataPembayaranRepository) {
        this.userRepository = userRepository;
        this.profilRepository = profilRepository;
        this.postinganRepository = postinganRepository;
        this.postinganBacaRepository = postinganBacaRepository;
        this.pesananRepository = pesananRepository;
        this.pemesananRepository = pemesananRepository;
        this.pembayaranRepository = pembayaranRepository;
        this.dataPembayaranRepository = dataPembayaranRepository;
    }

    // ---------- Jasa ----------

    public JasaTampil jasa(String idPostingan) {
        Postingan postingan = postinganRepository.findById(idPostingan).orElse(null);
        if (postingan == null) {
            return null;
        }
        User penjual = userRepository.findById(postingan.getIdPenjual()).orElse(null);
        String nama = namaPengguna(Set.of(postingan.getIdPenjual())).get(postingan.getIdPenjual());
        float rating = penjual == null ? 0f : penjual.getRataRataRating();
        long hargaAngka = postingan.getHarga() == null ? 0L : postingan.getHarga().longValue();
        return new JasaTampil(postingan.getIdPostingan(), postingan.getJudul(), postingan.getDeskripsi(),
                postingan.getKategori(), Teks.rupiah(postingan.getHarga()), hargaAngka,
                postingan.getIdPenjual(), nama, Teks.inisial(nama), Teks.rating(rating),
                Math.round(rating), penjual != null && "Penjual Terpercaya".equals(penjual.getBadgeKepercayaan()),
                rating > 0f, postingan.isStatusAktif());
    }

    // ---------- Pemesanan ----------

    public List<PemesananTampil> pemesananPembeli(String idPembeli) {
        return susunPemesanan(pemesananRepository.findByIdPembeliOrderByCreatedAtDesc(idPembeli));
    }

    public List<PemesananTampil> pemesananPenyedia(String idPenyedia) {
        List<String> idPostingan = postinganBacaRepository.cariIdByPenjual(idPenyedia);
        if (idPostingan.isEmpty()) {
            return List.of();
        }
        return susunPemesanan(pemesananRepository.findByIdPostinganInOrderByCreatedAtDesc(idPostingan));
    }

    public PemesananTampil pemesanan(String idPemesanan) {
        return pemesananRepository.findById(idPemesanan)
                .map(p -> susunPemesanan(List.of(p)))
                .flatMap(list -> list.stream().findFirst())
                .orElse(null);
    }

    public PemesananTampil pemesananBerdasarkanPesanan(String idPesanan) {
        return pemesananRepository.findByIdPesanan(idPesanan)
                .map(p -> susunPemesanan(List.of(p)))
                .flatMap(list -> list.stream().findFirst())
                .orElse(null);
    }

    private List<PemesananTampil> susunPemesanan(List<Pemesanan> daftar) {
        if (daftar.isEmpty()) {
            return List.of();
        }

        Set<String> idPesanan = daftar.stream().map(Pemesanan::getIdPesanan).collect(Collectors.toSet());
        Set<String> idPostingan = daftar.stream().map(Pemesanan::getIdPostingan).collect(Collectors.toSet());
        Set<String> idPengguna = new HashSet<>();
        daftar.forEach(p -> idPengguna.add(p.getIdPembeli()));

        Map<String, Pesanan> pesananMap = pesananRepository.findAllById(idPesanan).stream()
                .collect(Collectors.toMap(Pesanan::getIdPesanan, p -> p));
        Map<String, Postingan> postinganMap = postinganRepository.findAllById(idPostingan).stream()
                .collect(Collectors.toMap(Postingan::getIdPostingan, p -> p));
        postinganMap.values().forEach(p -> idPengguna.add(p.getIdPenjual()));

        Map<String, Pembayaran> pembayaranMap = new HashMap<>();
        for (String id : idPesanan) {
            pembayaranRepository.findByIdPesanan(id).ifPresent(b -> pembayaranMap.put(id, b));
        }

        Map<String, String> namaMap = namaPengguna(idPengguna);
        List<PemesananTampil> hasil = new ArrayList<>();

        for (Pemesanan pemesanan : daftar) {
            Pesanan pesanan = pesananMap.get(pemesanan.getIdPesanan());
            Postingan postingan = postinganMap.get(pemesanan.getIdPostingan());
            Pembayaran pembayaran = pesanan == null ? null : pembayaranMap.get(pesanan.getIdPesanan());

            String judul = postingan == null ? "Postingan " + pemesanan.getIdPostingan() : postingan.getJudul();
            String kategori = postingan == null ? "-" : postingan.getKategori();
            String hargaSatuan = postingan == null ? "-" : Teks.rupiah(postingan.getHarga());
            String total = totalPemesanan(pemesanan, pesanan, postingan);

            String idPenyedia = postingan == null ? null : postingan.getIdPenjual();
            String namaPembeli = namaMap.getOrDefault(pemesanan.getIdPembeli(), pemesanan.getIdPembeli());
            String namaPenyedia = idPenyedia == null ? "-" : namaMap.getOrDefault(idPenyedia, idPenyedia);

            String statusPesanan = pesanan == null ? "" : pesanan.getStatusPesanan();
            String statusPembayaran = pembayaran == null ? "" : pembayaran.getStatus();
            boolean bisaBayar = "diterima".equals(pemesanan.getStatusPesanan())
                    && "menunggu".equals(statusPesanan) && pembayaran == null;
            boolean bisaSelesai = "diterima".equals(pemesanan.getStatusPesanan())
                    && "dibayar".equals(statusPesanan)
                    && "dikonfirmasi".equals(statusPembayaran);

            hasil.add(new PemesananTampil(
                    pemesanan.getIdPemesanan(), pemesanan.getIdPesanan(), pemesanan.getIdPostingan(),
                    judul, kategori, hargaSatuan, pemesanan.getJumlah(), total,
                    waktu(pemesanan.getWaktuPesanan()), kosongJadi(pemesanan.getLokasi(), "-"),
                    kosongJadi(pemesanan.getCatatan(), "-"), waktu(pemesanan.getCreatedAt()),
                    pemesanan.getStatusPesanan(), labelPemesanan(pemesanan.getStatusPesanan()),
                    kelasPemesanan(pemesanan.getStatusPesanan()),
                    statusPesanan, labelPesanan(statusPesanan), kelasPesanan(statusPesanan),
                    pemesanan.getIdPembeli(), namaPembeli, Teks.inisial(namaPembeli),
                    idPenyedia, namaPenyedia, Teks.inisial(namaPenyedia),
                    pembayaran == null ? null : pembayaran.getIdPembayaran(), statusPembayaran,
                    pembayaran == null ? "Belum dibayar" : labelPembayaran(statusPembayaran),
                    pembayaran == null ? "badge-baru" : kelasPembayaran(statusPembayaran),
                    bisaBayar, pembayaran != null, bisaSelesai));
        }
        return hasil;
    }

    private String totalPemesanan(Pemesanan pemesanan, Pesanan pesanan, Postingan postingan) {
        if (pesanan != null && pesanan.getTotalHarga() != null) {
            return Teks.rupiah(pesanan.getTotalHarga());
        }
        if (postingan != null && postingan.getHarga() != null) {
            return Teks.rupiah(postingan.getHarga()
                    .multiply(BigDecimal.valueOf(pemesanan.getJumlah()))
                    .setScale(2, RoundingMode.HALF_UP));
        }
        return "-";
    }

    // ---------- Pembayaran ----------

    public List<PemesananTampil> tagihanPembeli(String idPembeli) {
        return pemesananPembeli(idPembeli).stream().filter(PemesananTampil::bisaBayar).toList();
    }

    public List<PembayaranTampil> pembayaranPembeli(String idPembeli) {
        List<Pembayaran> daftar = new ArrayList<>();
        for (Pesanan pesanan : pesananRepository.findByIdPembeliOrderByIdPesananDesc(idPembeli)) {
            pembayaranRepository.findByIdPesanan(pesanan.getIdPesanan()).ifPresent(daftar::add);
        }
        urutkanTerbaru(daftar);
        return susunPembayaran(daftar);
    }

    public List<PembayaranTampil> pembayaranPenyedia(String idPenyedia) {
        List<String> idPostingan = postinganBacaRepository.cariIdByPenjual(idPenyedia);
        if (idPostingan.isEmpty()) {
            return List.of();
        }
        List<Pemesanan> pemesanan = pemesananRepository.findByIdPostinganInOrderByCreatedAtDesc(idPostingan);
        List<Pembayaran> daftar = new ArrayList<>();
        for (Pemesanan p : pemesanan) {
            pembayaranRepository.findByIdPesanan(p.getIdPesanan()).ifPresent(daftar::add);
        }
        urutkanTerbaru(daftar);
        return susunPembayaran(daftar);
    }

    private void urutkanTerbaru(List<Pembayaran> daftar) {
        daftar.sort((a, b) -> {
            if (a.getTanggalBayar() == null) {
                return 1;
            }
            if (b.getTanggalBayar() == null) {
                return -1;
            }
            return b.getTanggalBayar().compareTo(a.getTanggalBayar());
        });
    }

    public PembayaranTampil pembayaran(String idPembayaran) {
        return pembayaranRepository.findById(idPembayaran)
                .map(b -> susunPembayaran(List.of(b)))
                .flatMap(list -> list.stream().findFirst())
                .orElse(null);
    }

    public Optional<PembayaranTampil> pembayaranBerdasarkanPesanan(String idPesanan) {
        return pembayaranRepository.findByIdPesanan(idPesanan)
                .map(b -> susunPembayaran(List.of(b)))
                .flatMap(list -> list.stream().findFirst());
    }

    private List<PembayaranTampil> susunPembayaran(List<Pembayaran> daftar) {
        if (daftar.isEmpty()) {
            return List.of();
        }

        Set<String> idPesanan = daftar.stream().map(Pembayaran::getIdPesanan).collect(Collectors.toSet());
        Map<String, Pesanan> pesananMap = pesananRepository.findAllById(idPesanan).stream()
                .collect(Collectors.toMap(Pesanan::getIdPesanan, p -> p));

        Map<String, Pemesanan> pemesananMap = new HashMap<>();
        for (String id : idPesanan) {
            pemesananRepository.findByIdPesanan(id).ifPresent(p -> pemesananMap.put(id, p));
        }

        Set<String> idPostingan = pemesananMap.values().stream()
                .map(Pemesanan::getIdPostingan).collect(Collectors.toSet());
        Map<String, Postingan> postinganMap = postinganRepository.findAllById(idPostingan).stream()
                .collect(Collectors.toMap(Postingan::getIdPostingan, p -> p));

        Set<String> idPengguna = new LinkedHashSet<>();
        daftar.forEach(b -> {
            Pesanan pesanan = pesananMap.get(b.getIdPesanan());
            if (pesanan != null) {
                idPengguna.add(pesanan.getIdPembeli());
            }
        });
        postinganMap.values().forEach(p -> idPengguna.add(p.getIdPenjual()));
        Map<String, String> namaMap = namaPengguna(idPengguna);

        List<PembayaranTampil> hasil = new ArrayList<>();
        for (Pembayaran pembayaran : daftar) {
            Pesanan pesanan = pesananMap.get(pembayaran.getIdPesanan());
            Pemesanan pemesanan = pemesananMap.get(pembayaran.getIdPesanan());
            Postingan postingan = pemesanan == null ? null : postinganMap.get(pemesanan.getIdPostingan());

            String idPembeli = pesanan == null ? (pemesanan == null ? null : pemesanan.getIdPembeli())
                    : pesanan.getIdPembeli();
            String idPenyedia = postingan == null ? null : postingan.getIdPenjual();
            String namaPembeli = idPembeli == null ? "-" : namaMap.getOrDefault(idPembeli, idPembeli);
            String namaPenyedia = idPenyedia == null ? "-" : namaMap.getOrDefault(idPenyedia, idPenyedia);
            String statusPesanan = pesanan == null ? "" : pesanan.getStatusPesanan();

            List<BuktiTampil> bukti = dataPembayaranRepository
                    .findByIdPembayaranOrderByTanggalSimpanDesc(pembayaran.getIdPembayaran())
                    .stream().map(this::petaBukti).toList();

            boolean menunggu = "menunggu_konfirmasi".equals(pembayaran.getStatus());
            boolean dikonfirmasi = "dikonfirmasi".equals(pembayaran.getStatus());

            hasil.add(new PembayaranTampil(
                    pembayaran.getIdPembayaran(), pembayaran.getIdPesanan(),
                    pemesanan == null ? null : pemesanan.getIdPemesanan(),
                    pemesanan == null ? null : pemesanan.getIdPostingan(),
                    postingan == null ? "Postingan tidak ditemukan" : postingan.getJudul(),
                    Teks.rupiah(pembayaran.getJumlah()), pembayaran.getMetode(),
                    pembayaran.getStatus(), labelPembayaran(pembayaran.getStatus()),
                    kelasPembayaran(pembayaran.getStatus()), waktu(pembayaran.getTanggalBayar()),
                    idPembeli, namaPembeli, Teks.inisial(namaPembeli),
                    idPenyedia, namaPenyedia, Teks.inisial(namaPenyedia),
                    statusPesanan, labelPesanan(statusPesanan), kelasPesanan(statusPesanan),
                    bukti.isEmpty() ? null : bukti.get(0).nilai(),
                    bukti.isEmpty() ? "-" : bukti.get(0).tanggal(), !bukti.isEmpty(),
                    menunggu, menunggu, menunggu || "perbaikan".equals(pembayaran.getStatus()),
                    dikonfirmasi && !"selesai".equals(statusPesanan), bukti));
        }
        return hasil;
    }

    private BuktiTampil petaBukti(DataPembayaran bukti) {
        String nilai = bukti.getBuktiPembayaran() == null ? "" : bukti.getBuktiPembayaran();
        boolean tautan = nilai.startsWith("http://") || nilai.startsWith("https://");
        return new BuktiTampil(bukti.getIdDataPembayaran(), nilai,
                waktu(bukti.getTanggalSimpan()), tautan);
    }

    // ---------- Nama pengguna ----------

    private Map<String, String> namaPengguna(Collection<String> idPengguna) {
        Set<String> ids = idPengguna.stream().filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> hasil = new HashMap<>();
        if (ids.isEmpty()) {
            return hasil;
        }

        for (Profil profil : profilRepository.findByIdUserIn(ids)) {
            if (profil.getNamaLengkap() != null && !profil.getNamaLengkap().isBlank()) {
                hasil.put(profil.getIdUser(), profil.getNamaLengkap());
            }
        }
        Set<String> sisa = ids.stream().filter(id -> !hasil.containsKey(id)).collect(Collectors.toSet());
        if (!sisa.isEmpty()) {
            for (User user : userRepository.findAllById(sisa)) {
                hasil.put(user.getIdUser(), user.getUsername());
            }
        }
        for (String id : ids) {
            hasil.putIfAbsent(id, id);
        }
        return hasil;
    }

    // ---------- Label tampilan ----------

    private String labelPemesanan(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "menunggu" -> "Menunggu persetujuan";
            case "diterima" -> "Diterima";
            case "ditolak" -> "Ditolak";
            default -> status;
        };
    }

    private String kelasPemesanan(String status) {
        if (status == null) {
            return "badge-baru";
        }
        return switch (status) {
            case "menunggu" -> "badge-menunggu";
            case "diterima" -> "badge-diterima";
            case "ditolak" -> "badge-ditolak";
            default -> "badge-baru";
        };
    }

    private String labelPesanan(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "menunggu" -> "Menunggu pembayaran";
            case "dibayar" -> "Sudah dibayar";
            case "selesai" -> "Selesai";
            case "dibatalkan" -> "Dibatalkan";
            default -> status;
        };
    }

    private String kelasPesanan(String status) {
        if (status == null) {
            return "badge-baru";
        }
        return switch (status) {
            case "menunggu" -> "badge-menunggu";
            case "dibayar" -> "badge-dibayar";
            case "selesai" -> "badge-selesai";
            case "dibatalkan" -> "badge-dibatalkan";
            default -> "badge-baru";
        };
    }

    private String labelPembayaran(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "menunggu_konfirmasi" -> "Menunggu konfirmasi";
            case "perbaikan" -> "Perlu perbaikan";
            case "dikonfirmasi" -> "Terkonfirmasi";
            default -> status;
        };
    }

    private String kelasPembayaran(String status) {
        if (status == null) {
            return "badge-baru";
        }
        return switch (status) {
            case "menunggu_konfirmasi" -> "badge-menunggu";
            case "perbaikan" -> "badge-perbaikan";
            case "dikonfirmasi" -> "badge-dikonfirmasi";
            default -> "badge-baru";
        };
    }

    private String waktu(LocalDateTime waktu) {
        return waktu == null ? "-" : WAKTU.format(waktu);
    }

    private String kosongJadi(String nilai, String cadangan) {
        return nilai == null || nilai.isBlank() ? cadangan : nilai;
    }
}
