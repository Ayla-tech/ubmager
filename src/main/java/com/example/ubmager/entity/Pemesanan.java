package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "pemesanan")
public class Pemesanan {
    @Id
    @Column(name = "id_pemesanan", length = 10, nullable = false)
    private String idPemesanan;

    @Column(name = "id_pesanan", length = 10, nullable = false, unique = true)
    private String idPesanan;

    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "id_pembeli", length = 10, nullable = false)
    private String idPembeli;

    @Column(name = "jumlah", nullable = false)
    private int jumlah;

    @Column(name = "waktu_pesanan")
    private LocalDateTime waktuPesanan;

    @Column(name = "lokasi", length = 255)
    private String lokasi;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "status_pesanan", length = 20, nullable = false)
    private String statusPesanan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getIdPemesanan() { return idPemesanan; }
    public void setIdPemesanan(String idPemesanan) { this.idPemesanan = idPemesanan; }
    public String getIdPesanan() { return idPesanan; }
    public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }
    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public String getIdPembeli() { return idPembeli; }
    public void setIdPembeli(String idPembeli) { this.idPembeli = idPembeli; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public LocalDateTime getWaktuPesanan() { return waktuPesanan; }
    public void setWaktuPesanan(LocalDateTime waktuPesanan) { this.waktuPesanan = waktuPesanan; }
    public String getLokasi() { return lokasi; }
    public void setLokasi(String lokasi) { this.lokasi = lokasi; }
    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }
    public String getStatusPesanan() { return statusPesanan; }
    public void setStatusPesanan(String statusPesanan) { this.statusPesanan = statusPesanan; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
