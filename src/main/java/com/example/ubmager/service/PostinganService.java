package com.example.ubmager.service;

import com.example.ubmager.dto.Hasil;
import com.example.ubmager.entity.Postingan;
import com.example.ubmager.repository.DataMediaRepository;
import com.example.ubmager.repository.PostinganRepository;
import com.example.ubmager.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PostinganService {

    private final PostinganRepository postinganRepository;
    private final DataMediaRepository dataMediaRepository;
    private final IdGenerator idGenerator;

    public PostinganService(PostinganRepository postinganRepository, DataMediaRepository dataMediaRepository, IdGenerator idGenerator) {
        this.postinganRepository = postinganRepository;
        this.dataMediaRepository = dataMediaRepository;
        this.idGenerator = idGenerator;
    }

    public Hasil prosesKelolaPostingan(String aksi, Map<String, Object> data) {
        switch (aksi) {
            case "CREATE": {
                String judul = data.getOrDefault("judul", "").toString().trim();
                String deskripsi = data.getOrDefault("deskripsi", "").toString().trim();
                BigDecimal harga = new BigDecimal(data.getOrDefault("harga", "0").toString());
                LocalDateTime jamTayang = LocalDateTime.parse(data.get("jam_tayang").toString());

                if (judul.isEmpty() || deskripsi.isEmpty() || harga.compareTo(BigDecimal.ZERO) <= 0 || !jamTayang.isAfter(LocalDateTime.now())) {
                    return new Hasil(false, "Data postingan tidak lengkap atau jam tayang tidak valid", null);
                }

                Postingan post = new Postingan();
                post.setIdPostingan(idGenerator.buat("PST", "postingan", "id_postingan"));
                post.setIdPenjual(data.get("id_penjual").toString());
                post.setJudul(judul);
                post.setDeskripsi(deskripsi);
                post.setHarga(harga);
                post.setKategori(data.get("kategori").toString());
                post.setJamTayang(jamTayang);
                post.setStatusAktif(true);
                post.setCreatedAt(LocalDateTime.now());
                post.setUpdatedAt(LocalDateTime.now());

                postinganRepository.save(post);
                return new Hasil(true, "Postingan berhasil dibuat", post);
            }

            case "READ": {
                List<Postingan> daftar = postinganRepository.findByIdPenjualOrderByCreatedAtDesc(data.get("id_penjual").toString());
                for (Postingan p : daftar) {
                    if (p.getStatusAktif() && p.getJamTayang().isBefore(LocalDateTime.now())) {
                        p.setStatusAktif(false);
                        postinganRepository.save(p);
                    }
                }
                return new Hasil(true, "Daftar postingan ditemukan", daftar);
            }

            case "UPDATE": {
                String idPostingan = data.get("id_postingan").toString();
                String idPenjual = data.get("id_penjual").toString();
                Optional<Postingan> opt = postinganRepository.findById(idPostingan);

                if (opt.isEmpty() || !opt.get().getIdPenjual().equals(idPenjual)) {
                    return new Hasil(false, "Postingan tidak ditemukan atau akses ditolak", null);
                }

                BigDecimal harga = new BigDecimal(data.getOrDefault("harga", "0").toString());
                LocalDateTime jamTayang = LocalDateTime.parse(data.get("jam_tayang").toString());
                if (harga.compareTo(BigDecimal.ZERO) <= 0 || !jamTayang.isAfter(LocalDateTime.now())) {
                    return new Hasil(false, "Data pembaruan tidak valid", null);
                }

                Postingan post = opt.get();
                post.setJudul(data.get("judul").toString().trim());
                post.setDeskripsi(data.get("deskripsi").toString().trim());
                post.setHarga(harga);
                post.setKategori(data.get("kategori").toString());
                post.setJamTayang(jamTayang);
                post.setStatusAktif(true);
                post.setUpdatedAt(LocalDateTime.now());

                postinganRepository.save(post);
                return new Hasil(true, "Postingan berhasil diperbarui", post);
            }

            case "DELETE": {
                String idPostingan = data.get("id_postingan").toString();
                String idPenjual = data.get("id_penjual").toString();
                Optional<Postingan> opt = postinganRepository.findById(idPostingan);

                if (opt.isEmpty() || !opt.get().getIdPenjual().equals(idPenjual)) {
                    return new Hasil(false, "Postingan tidak ditemukan", null);
                }

                dataMediaRepository.deleteByIdPostingan(idPostingan);
                postinganRepository.deleteById(idPostingan);
                return new Hasil(true, "Postingan berhasil dihapus", null);
            }

            default:
                return new Hasil(false, "Aksi tidak valid", null);
        }
    }

    public Hasil prosesPencarian(String kataKunci, String kategori, Double hargaMin, Double hargaMax) {
        if (hargaMin != null && hargaMax != null && hargaMax < hargaMin) {
            return new Hasil(false, "Harga maksimum tidak boleh lebih kecil dari harga minimum", null);
        }
        BigDecimal min = hargaMin != null ? BigDecimal.valueOf(hargaMin) : null;
        BigDecimal max = hargaMax != null ? BigDecimal.valueOf(hargaMax) : null;
        List<Postingan> hasil = postinganRepository.cariDenganFilter(kataKunci, kategori, min, max);
        return new Hasil(true, "Pencarian berhasil", hasil);
    }
}