package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pesanan")
public class Pesanan {
    @Id
    @Column(name = "id_pesanan", length = 10, nullable = false)
    private String idPesanan;

    @Column(name = "id_pembeli", length = 10, nullable = false)
    private String idPembeli;

    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "status_pesanan", length = 15, nullable = false)
    private String statusPesanan;

    @Column(name = "tanggal_pesan")
    private LocalDateTime tanggalPesan;

    @Column(name = "total_harga", precision = 12, scale = 2)
    private BigDecimal totalHarga;

    public String getIdPesanan() { return idPesanan; }
    public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }
    public String getIdPembeli() { return idPembeli; }
    public void setIdPembeli(String idPembeli) { this.idPembeli = idPembeli; }
    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public String getStatusPesanan() { return statusPesanan; }
    public void setStatusPesanan(String statusPesanan) { this.statusPesanan = statusPesanan; }
    public LocalDateTime getTanggalPesan() { return tanggalPesan; }
    public void setTanggalPesan(LocalDateTime tanggalPesan) { this.tanggalPesan = tanggalPesan; }
    public BigDecimal getTotalHarga() { return totalHarga; }
    public void setTotalHarga(BigDecimal totalHarga) { this.totalHarga = totalHarga; }
}
