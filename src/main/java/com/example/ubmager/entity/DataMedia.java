package com.example.ubmager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "datamedia")
public class DataMedia {

    @Id
    @Column(name = "id_media", length = 10)
    private String idMedia;

    @Column(name = "id_postingan", length = 10, nullable = false)
    private String idPostingan;

    @Column(name = "url_file", nullable = false)
    private String urlFile;

    @Column(name = "tipe_file", length = 10, nullable = false)
    private String tipeFile;

    @Column(name = "ukuran_byte", nullable = false)
    private Integer ukuranByte;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getIdMedia() { return idMedia; }
    public void setIdMedia(String idMedia) { this.idMedia = idMedia; }
    public String getIdPostingan() { return idPostingan; }
    public void setIdPostingan(String idPostingan) { this.idPostingan = idPostingan; }
    public String getUrlFile() { return urlFile; }
    public void setUrlFile(String urlFile) { this.urlFile = urlFile; }
    public String getTipeFile() { return tipeFile; }
    public void setTipeFile(String tipeFile) { this.tipeFile = tipeFile; }
    public Integer getUkuranByte() { return ukuranByte; }
    public void setUkuranByte(Integer ukuranByte) { this.ukuranByte = ukuranByte; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}