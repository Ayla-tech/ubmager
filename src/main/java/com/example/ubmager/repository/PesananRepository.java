package com.example.ubmager.repository;

import com.example.ubmager.entity.Pesanan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesananRepository extends JpaRepository<Pesanan, String> {
    boolean existsByIdPembeli(String idPembeli);
}
