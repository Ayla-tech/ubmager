package com.example.ubmager.repository;

import com.example.ubmager.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsernameOrEmailAtauHp(String username, String emailAtauHp);
    boolean existsByUsername(String username);
    boolean existsByEmailAtauHp(String emailAtauHp);
    Optional<User> findByUsernameOrEmailAtauHp(String username, String emailAtauHp);
}
