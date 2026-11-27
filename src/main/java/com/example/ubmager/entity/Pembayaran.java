package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pembayaran")
public class Pembayaran {
    @Id
    @Column(name = "id_pembayaran", length = 10, nullable = false)
    private String idPembayaran;

    @Column(name = "id_pesanan", length = 10, nullable = false, unique = true)
    private String idPesanan;

    @Column(name = "metode", length = 50, nullable = false)
    private String metode;

    @Column(name = "jumlah", precision = 12, scale = 2, nullable = false)
    private BigDecimal jumlah;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "tanggal_bayar", nullable = false)
    private LocalDateTime tanggalBayar;

    public String getIdPembayaran() { return idPembayaran; }
    public void setIdPembayaran(String idPembayaran) { this.idPembayaran = idPembayaran; }
    public String getIdPesanan() { return idPesanan; }
    public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }
    public String getMetode() { return metode; }
    public void setMetode(String metode) { this.metode = metode; }
    public BigDecimal getJumlah() { return jumlah; }
    public void setJumlah(BigDecimal jumlah) { this.jumlah = jumlah; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTanggalBayar() { return tanggalBayar; }
    public void setTanggalBayar(LocalDateTime tanggalBayar) { this.tanggalBayar = tanggalBayar; }
}
