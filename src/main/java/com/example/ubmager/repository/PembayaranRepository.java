package com.example.ubmager.repository;

import com.example.ubmager.entity.Pembayaran;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PembayaranRepository extends JpaRepository<Pembayaran, String> {
    Optional<Pembayaran> findByIdPesanan(String idPesanan);
    boolean existsByIdPesanan(String idPesanan);
}
