package com.example.ubmager.repository;

import com.example.ubmager.entity.DataPembayaran;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataPembayaranRepository extends JpaRepository<DataPembayaran, String> {
    List<DataPembayaran> findByIdPembayaranOrderByTanggalSimpanDesc(String idPembayaran);
    boolean existsByIdPembayaran(String idPembayaran);
}
