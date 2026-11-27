package com.example.ubmager.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Integrasi baca-saja ke tabel POSTINGAN milik modul lain.
 *
 * Entity Postingan belum tersedia di project ini, sehingga repository ini sengaja
 * tidak memetakan entity baru dan tidak menyediakan operasi tulis. Hanya kolom
 * yang dibutuhkan modul Pemesanan/Pembayaran yang dibaca.
 */
@Repository
public class PostinganBacaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Info> cari(String idPostingan) {
        if (idPostingan == null || idPostingan.isBlank()) {
            return Optional.empty();
        }

        List<?> hasil = entityManager.createNativeQuery(
                "SELECT id_postingan, id_penjual, harga, status_aktif FROM postingan"
                + " WHERE id_postingan = :idPostingan")
                .setParameter("idPostingan", idPostingan)
                .getResultList();

        if (hasil.isEmpty()) {
            return Optional.empty();
        }

        Object[] baris = (Object[]) hasil.get(0);
        Object harga = baris[2];
        Object aktif = baris[3];
        return Optional.of(new Info(
                String.valueOf(baris[0]),
                String.valueOf(baris[1]),
                harga == null ? null : new BigDecimal(harga.toString()),
                aktifBoolean(aktif)));
    }

    public List<String> cariIdByPenjual(String idPenjual) {
        if (idPenjual == null || idPenjual.isBlank()) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<String> hasil = entityManager.createNativeQuery(
                "SELECT id_postingan FROM postingan WHERE id_penjual = :idPenjual")
                .setParameter("idPenjual", idPenjual)
                .getResultList();
        return hasil;
    }

    private boolean aktifBoolean(Object nilai) {
        if (nilai instanceof Boolean b) {
            return b;
        }
        if (nilai == null) {
            return false;
        }
        String teks = nilai.toString().trim();
        return teks.equalsIgnoreCase("true") || teks.equalsIgnoreCase("aktif");
    }

    public record Info(String idPostingan, String idPenjual, BigDecimal harga, boolean statusAktif) {
    }
}
