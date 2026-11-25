package com.example.ubmager.repository;

import com.example.ubmager.entity.Pesanan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PesananRepository extends JpaRepository<Pesanan, String> {
    boolean existsByIdPembeli(String idPembeli);
    List<Pesanan> findByIdPembeliOrderByIdPesananDesc(String idPembeli);
}
