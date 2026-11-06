package com.example.ubmager.repository;

import com.example.ubmager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsernameOrEmailAtauHp(String username, String emailAtauHp);
}