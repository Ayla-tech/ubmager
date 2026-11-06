package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "datareview")
public class DataReview {
    @Id
    @Column(name = "id_review", length = 10, nullable = false)
    private String idReview;

    @Column(name = "id_pesanan", length = 10, nullable = false)
    private String idPesanan;

    @Column(name = "id_reviewer", length = 10, nullable = false)
    private String idReviewer;

    @Column(name = "id_target", length = 10, nullable = false)
    private String idTarget;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "komentar", columnDefinition = "TEXT")
    private String komentar;

    @Column(name = "tanggal_review", nullable = false)
    private LocalDateTime tanggalReview;

    public String getIdReview() { return idReview; }
    public void setIdReview(String idReview) { this.idReview = idReview; }
    public String getIdPesanan() { return idPesanan; }
    public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }
    public String getIdReviewer() { return idReviewer; }
    public void setIdReviewer(String idReviewer) { this.idReviewer = idReviewer; }
    public String getIdTarget() { return idTarget; }
    public void setIdTarget(String idTarget) { this.idTarget = idTarget; }
    public short getRating() { return rating; }
    public void setRating(short rating) { this.rating = rating; }
    public String getKomentar() { return komentar; }
    public void setKomentar(String komentar) { this.komentar = komentar; }
    public LocalDateTime getTanggalReview() { return tanggalReview; }
    public void setTanggalReview(LocalDateTime tanggalReview) { this.tanggalReview = tanggalReview; }
}