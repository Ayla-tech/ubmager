package com.example.ubmager.repository;

import com.example.ubmager.entity.ProdukKeranjang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdukKeranjangRepository extends JpaRepository<ProdukKeranjang, String> {
    List<ProdukKeranjang> findByIdKeranjang(String idKeranjang);
    List<ProdukKeranjang> findByIdKeranjangAndDipilihTrue(String idKeranjang);
    void deleteByIdKeranjangAndTidakTersediaTrue(String idKeranjang);
}