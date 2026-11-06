package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    public String getIdPesanan() { return idPesanan; }
    public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }
    public String getIdPembeli() { return idPembeli; }
    public void setIdPembeli(String idPembeli) { this.idPembeli = idPembeli; }
    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public String getStatusPesanan() { return statusPesanan; }
    public void setStatusPesanan(String statusPesanan) { this.statusPesanan = statusPesanan; }
}
