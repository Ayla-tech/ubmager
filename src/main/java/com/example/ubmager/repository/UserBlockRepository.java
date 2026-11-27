package com.example.ubmager.repository;

import com.example.ubmager.entity.UserBlock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, String> {
    Optional<UserBlock> findByIdUserAndIdUserDiblokir(String idUser, String idUserDiblokir);
    boolean existsByIdUserAndIdUserDiblokirAndStatusBlokir(String idUser, String idUserDiblokir, boolean statusBlokir);
}
