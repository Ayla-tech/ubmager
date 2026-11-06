package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profil")
public class Profil {
    @Id
    @Column(name = "id_profil", length = 10, nullable = false)
    private String idProfil;

    @Column(name = "id_user", length = 10, nullable = false)
    private String idUser;

    @Column(name = "nama_lengkap", length = 100, nullable = false)
    private String namaLengkap;

    @Column(name = "foto_profil", length = 255)
    private String fotoProfil;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    public String getIdProfil() { return idProfil; }
    public void setIdProfil(String idProfil) { this.idProfil = idProfil; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getNamaLengkap() { return namaLengkap; }
    public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }
    public String getFotoProfil() { return fotoProfil; }
    public void setFotoProfil(String fotoProfil) { this.fotoProfil = fotoProfil; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
