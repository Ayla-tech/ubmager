package com.example.ubmager.repository;

import com.example.ubmager.entity.Keranjang;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeranjangRepository extends JpaRepository<Keranjang, String> {
    Optional<Keranjang> findByIdUser(String idUser);
}