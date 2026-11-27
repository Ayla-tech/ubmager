package com.example.ubmager.repository;

import com.example.ubmager.entity.Notifikasi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotifikasiRepository extends JpaRepository<Notifikasi, String> {
    List<Notifikasi> findByIdUserOrderByCreatedAtDesc(String idUser);
    long countByIdUserAndDibaca(String idUser, boolean dibaca);

    @Modifying
    @Query("UPDATE Notifikasi n SET n.dibaca = true WHERE n.idUser = :idUser AND n.dibaca = false")
    int tandaiSemuaDibaca(@Param("idUser") String idUser);
}
