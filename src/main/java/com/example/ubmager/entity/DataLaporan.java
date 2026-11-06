package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "datalaporan")
public class DataLaporan {
    @Id
    @Column(name = "id_laporan", length = 10, nullable = false)
    private String idLaporan;

    @Column(name = "id_review", length = 10, nullable = false)
    private String idReview;

    @Column(name = "id_pelapor", length = 10, nullable = false)
    private String idPelapor;

    @Column(name = "alasan_laporan", columnDefinition = "TEXT", nullable = false)
    private String alasanLaporan;

    @Column(name = "status_laporan", length = 15, nullable = false)
    private String statusLaporan;

    @Column(name = "tanggal_laporan", nullable = false)
    private LocalDateTime tanggalLaporan;

    public String getIdLaporan() { return idLaporan; }
    public void setIdLaporan(String idLaporan) { this.idLaporan = idLaporan; }
    public String getIdReview() { return idReview; }
    public void setIdReview(String idReview) { this.idReview = idReview; }
    public String getIdPelapor() { return idPelapor; }
    public void setIdPelapor(String idPelapor) { this.idPelapor = idPelapor; }
    public String getAlasanLaporan() { return alasanLaporan; }
    public void setAlasanLaporan(String alasanLaporan) { this.alasanLaporan = alasanLaporan; }
    public String getStatusLaporan() { return statusLaporan; }
    public void setStatusLaporan(String statusLaporan) { this.statusLaporan = statusLaporan; }
    public LocalDateTime getTanggalLaporan() { return tanggalLaporan; }
    public void setTanggalLaporan(LocalDateTime tanggalLaporan) { this.tanggalLaporan = tanggalLaporan; }
}
