package com.example.ubmager.repository;

import com.example.ubmager.entity.Postingan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostinganRepository extends JpaRepository<Postingan, String> {
    List<Postingan> findByStatusAktifTrueOrderByCreatedAtDesc();
    boolean existsByIdPenjual(String idPenjual);
}
