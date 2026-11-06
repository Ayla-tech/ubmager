package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id_user", length = 10, nullable = false)
    private String idUser;

    @Column(name = "username", length = 50, unique = true, nullable = false)
    private String username;

    @Column(name = "email_atau_hp", length = 100, unique = true, nullable = false)
    private String emailAtauHp;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "status_akun", length = 20, nullable = false)
    private String statusAkun;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at", nullable = false)
    private LocalDateTime updateAt;

    @Column(name = "rata_rata_rating", nullable = false)
    private float rataRataRating;

    @Column(name = "badge_kepercayaan", length = 30, nullable = false)
    private String badgeKepercayaan;

    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmailAtauHp() { return emailAtauHp; }
    public void setEmailAtauHp(String emailAtauHp) { this.emailAtauHp = emailAtauHp; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStatusAkun() { return statusAkun; }
    public void setStatusAkun(String statusAkun) { this.statusAkun = statusAkun; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdateAt() { return updateAt; }
    public void setUpdateAt(LocalDateTime updateAt) { this.updateAt = updateAt; }
    public float getRataRataRating() { return rataRataRating; }
    public void setRataRataRating(float rataRataRating) { this.rataRataRating = rataRataRating; }
    public String getBadgeKepercayaan() { return badgeKepercayaan; }
    public void setBadgeKepercayaan(String badgeKepercayaan) { this.badgeKepercayaan = badgeKepercayaan; }
}
