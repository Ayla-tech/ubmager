package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "produk_keranjang")
public class ProdukKeranjang {

    @Id
    @Column(name = "id_produk", length = 10, nullable = false)
    private String idProduk;

    @Column(name = "id_keranjang", length = 10, nullable = false)
    private String idKeranjang;

    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "jumlah", nullable = false)
    private int jumlah = 1;

    @Column(name = "jadwal")
    private LocalDateTime jadwal;

    @Column(name = "lokasi", length = 100)
    private String lokasi;

    @Column(name = "catatan", columnDefinition = "TEXT")
    private String catatan;

    @Column(name = "harga_satuan", nullable = false)
    private double hargaSatuan;

    @Column(name = "dipilih", nullable = false)
    private boolean dipilih = true;

    @Column(name = "tidak_tersedia", nullable = false)
    private boolean tidakTersedia = false;

    public String getIdProduk() { return idProduk; }
    public void setIdProduk(String idProduk) { this.idProduk = idProduk; }
    public String getIdKeranjang() { return idKeranjang; }
    public void setIdKeranjang(String idKeranjang) { this.idKeranjang = idKeranjang; }
    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public LocalDateTime getJadwal() { return jadwal; }
    public void setJadwal(LocalDateTime jadwal) { this.jadwal = jadwal; }
    public String getLokasi() { return lokasi; }
    public void setLokasi(String lokasi) { this.lokasi = lokasi; }
    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan; }
    public double getHargaSatuan() { return hargaSatuan; }
    public void setHargaSatuan(double hargaSatuan) { this.hargaSatuan = hargaSatuan; }
    public boolean isDipilih() { return dipilih; }
    public void setDipilih(boolean dipilih) { this.dipilih = dipilih; }
    public boolean isTidakTersedia() { return tidakTersedia; }
    public void setTidakTersedia(boolean tidakTersedia) { this.tidakTersedia = tidakTersedia; }
}