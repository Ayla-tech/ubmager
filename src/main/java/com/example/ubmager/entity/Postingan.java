package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Versi minimal. Milik fitur CRUD Post, boleh diganti versi anggota terkait.
@Entity
@Table(name = "postingan")
public class Postingan {
    @Id
    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "id_penjual", length = 10, nullable = false)
    private String idPenjual;

    @Column(name = "judul", length = 100, nullable = false)
    private String judul;

    @Column(name = "deskripsi", columnDefinition = "TEXT", nullable = false)
    private String deskripsi;

    @Column(name = "harga", nullable = false)
    private BigDecimal harga;

    @Column(name = "kategori", length = 50, nullable = false)
    private String kategori;

    @Column(name = "jam_tayang", nullable = false)
    private LocalDateTime jamTayang;

    @Column(name = "status_aktif", nullable = false)
    private boolean statusAktif;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public String getIdPenjual() { return idPenjual; }
    public void setIdPenjual(String idPenjual) { this.idPenjual = idPenjual; }
    public String getJudul() { return judul; }
    public void setJudul(String judul) { this.judul = judul; }
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public BigDecimal getHarga() { return harga; }
    public void setHarga(BigDecimal harga) { this.harga = harga; }
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public LocalDateTime getJamTayang() { return jamTayang; }
    public void setJamTayang(LocalDateTime jamTayang) { this.jamTayang = jamTayang; }
    public boolean isStatusAktif() { return statusAktif; }
    public void setStatusAktif(boolean statusAktif) { this.statusAktif = statusAktif; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
