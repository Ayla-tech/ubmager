package com.example.ubmager.repository;

import com.example.ubmager.entity.Pemesanan;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PemesananRepository extends JpaRepository<Pemesanan, String> {
    Optional<Pemesanan> findByIdPesanan(String idPesanan);
    boolean existsByIdPesanan(String idPesanan);
    List<Pemesanan> findByIdPembeliOrderByCreatedAtDesc(String idPembeli);
    List<Pemesanan> findByIdPostinganInOrderByCreatedAtDesc(Collection<String> idPostingan);
}
