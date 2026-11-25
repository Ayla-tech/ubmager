package com.example.ubmager.repository;

import com.example.ubmager.entity.Profil;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfilRepository extends JpaRepository<Profil, String> {
    Optional<Profil> findByIdUser(String idUser);
    List<Profil> findByIdUserIn(Collection<String> idUser);

    @Modifying
    @Query("DELETE FROM Profil p WHERE p.idUser = :idUser")
    void hapusByIdUser(@Param("idUser") String idUser);
}
