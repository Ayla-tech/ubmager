package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifikasi")
public class Notifikasi {
    @Id
    @Column(name = "id_notifikasi", length = 10, nullable = false)
    private String idNotifikasi;

    @Column(name = "id_user", length = 10, nullable = false)
    private String idUser;

    @Column(name = "tipe", length = 20, nullable = false)
    private String tipe;

    @Column(name = "pesan", columnDefinition = "TEXT", nullable = false)
    private String pesan;

    @Column(name = "id_referensi", length = 10, nullable = false)
    private String idReferensi;

    @Column(name = "url_tujuan", length = 255, nullable = false)
    private String urlTujuan;

    @Column(name = "dibaca", nullable = false)
    private boolean dibaca = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getIdNotifikasi() { return idNotifikasi; }
    public void setIdNotifikasi(String idNotifikasi) { this.idNotifikasi = idNotifikasi; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }
    public String getPesan() { return pesan; }
    public void setPesan(String pesan) { this.pesan = pesan; }
    public String getIdReferensi() { return idReferensi; }
    public void setIdReferensi(String idReferensi) { this.idReferensi = idReferensi; }
    public String getUrlTujuan() { return urlTujuan; }
    public void setUrlTujuan(String urlTujuan) { this.urlTujuan = urlTujuan; }
    public boolean isDibaca() { return dibaca; }
    public void setDibaca(boolean dibaca) { this.dibaca = dibaca; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}