package com.example.ubmager.repository;

import com.example.ubmager.entity.Postingan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface PostinganRepository extends JpaRepository<Postingan, String> {

    List<Postingan> findByIdPenjualOrderByCreatedAtDesc(String idPenjual);

    List<Postingan> findByStatusAktifTrueOrderByCreatedAtDesc();

    boolean existsByIdPenjual(String idPenjual);

    @Query("SELECT p FROM Postingan p WHERE p.statusAktif = true " +
           "AND (:kataKunci = '' OR LOWER(p.judul) LIKE LOWER(CONCAT('%', :kataKunci, '%')) OR LOWER(p.deskripsi) LIKE LOWER(CONCAT('%', :kataKunci, '%'))) " +
           "AND (:kategori = '' OR p.kategori = :kategori) " +
           "AND (:hargaMin IS NULL OR p.harga >= :hargaMin) " +
           "AND (:hargaMax IS NULL OR p.harga <= :hargaMax) " +
           "ORDER BY p.createdAt DESC")
    List<Postingan> cariDenganFilter(
        @Param("kataKunci") String kataKunci,
        @Param("kategori") String kategori,
        @Param("hargaMin") BigDecimal hargaMin,
        @Param("hargaMax") BigDecimal hargaMax
    );
}