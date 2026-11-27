package com.example.ubmager.repository;

import com.example.ubmager.entity.Transaksi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransaksiRepository extends JpaRepository<Transaksi, String> {

    @Query("SELECT t FROM Transaksi t WHERE t.idUser = :idUser "
         + "AND (:kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) "
         + "AND LOWER(t.judulPostingan) LIKE LOWER(CONCAT('%', :kataKunci, '%')) "
         + "ORDER BY t.tanggalTransaksi DESC")
    List<Transaksi> cariUntukPembeli(@Param("idUser") String idUser,
            @Param("kategori") String kategori, @Param("kataKunci") String kataKunci);

    @Query("SELECT t FROM Transaksi t WHERE t.idPostingan IN "
         + "(SELECT p.idPostingan FROM Postingan p WHERE p.idPenjual = :idPenjual) "
         + "AND (:kategori = '' OR LOWER(t.kategori) = LOWER(:kategori)) "
         + "AND LOWER(t.judulPostingan) LIKE LOWER(CONCAT('%', :kataKunci, '%')) "
         + "ORDER BY t.tanggalTransaksi DESC")
    List<Transaksi> cariUntukPenjual(@Param("idPenjual") String idPenjual,
            @Param("kategori") String kategori, @Param("kataKunci") String kataKunci);
}