package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "datapembayaran")
public class DataPembayaran {
    @Id
    @Column(name = "id_data_pembayaran", length = 10, nullable = false)
    private String idDataPembayaran;

    @Column(name = "id_pembayaran", length = 10, nullable = false)
    private String idPembayaran;

    @Column(name = "bukti_pembayaran", length = 255)
    private String buktiPembayaran;

    @Column(name = "tanggal_simpan", nullable = false)
    private LocalDateTime tanggalSimpan;

    public String getIdDataPembayaran() { return idDataPembayaran; }
    public void setIdDataPembayaran(String idDataPembayaran) { this.idDataPembayaran = idDataPembayaran; }
    public String getIdPembayaran() { return idPembayaran; }
    public void setIdPembayaran(String idPembayaran) { this.idPembayaran = idPembayaran; }
    public String getBuktiPembayaran() { return buktiPembayaran; }
    public void setBuktiPembayaran(String buktiPembayaran) { this.buktiPembayaran = buktiPembayaran; }
    public LocalDateTime getTanggalSimpan() { return tanggalSimpan; }
    public void setTanggalSimpan(LocalDateTime tanggalSimpan) { this.tanggalSimpan = tanggalSimpan; }
}
