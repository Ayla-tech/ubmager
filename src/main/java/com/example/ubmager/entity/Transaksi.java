package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaksi")
public class Transaksi {

    @Id
    @Column(name = "id_transaksi", length = 10, nullable = false)
    private String idTransaksi;

    @Column(name = "id_user", length = 10, nullable = false)
    private String idUser;

    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "judul_postingan", length = 100, nullable = false)
    private String judulPostingan;

    @Column(name = "kategori", length = 50, nullable = false)
    private String kategori;

    @Column(name = "jumlah", nullable = false)
    private int jumlah;

    @Column(name = "total_harga", nullable = false)
    private double totalHarga;

    @Column(name = "tanggal_transaksi", nullable = false)
    private LocalDateTime tanggalTransaksi;

    @Column(name = "status_transaksi", length = 15, nullable = false)
    private String statusTransaksi;

    public String getIdTransaksi() { return idTransaksi; }
    public void setIdTransaksi(String idTransaksi) { this.idTransaksi = idTransaksi; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public String getJudulPostingan() { return judulPostingan; }
    public void setJudulPostingan(String judulPostingan) { this.judulPostingan = judulPostingan; }
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public double getTotalHarga() { return totalHarga; }
    public void setTotalHarga(double totalHarga) { this.totalHarga = totalHarga; }
    public LocalDateTime getTanggalTransaksi() { return tanggalTransaksi; }
    public void setTanggalTransaksi(LocalDateTime tanggalTransaksi) { this.tanggalTransaksi = tanggalTransaksi; }
    public String getStatusTransaksi() { return statusTransaksi; }
    public void setStatusTransaksi(String statusTransaksi) { this.statusTransaksi = statusTransaksi; }
}
