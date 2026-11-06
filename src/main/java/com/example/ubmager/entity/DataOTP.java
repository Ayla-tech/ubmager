package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataotp")
public class DataOTP {
    @Id
    @Column(name = "id_otp", length = 10, nullable = false)
    private String idOtp;

    @Column(name = "email_atau_hp", length = 100, nullable = false)
    private String emailAtauHp;

    @Column(name = "kode_otp", length = 6, nullable = false)
    private String kodeOtp;

    @Column(name = "waktu_dibuat", nullable = false)
    private LocalDateTime waktuDibuat;

    @Column(name = "waktu_kadaluarsa", nullable = false)
    private LocalDateTime waktuKadaluarsa;

    @Column(name = "jumlah_salah", nullable = false)
    private short jumlahSalah;

    @Column(name = "status_otp", length = 15, nullable = false)
    private String statusOtp;

    public String getIdOtp() { return idOtp; }
    public void setIdOtp(String idOtp) { this.idOtp = idOtp; }
    public String getEmailAtauHp() { return emailAtauHp; }
    public void setEmailAtauHp(String emailAtauHp) { this.emailAtauHp = emailAtauHp; }
    public String getKodeOtp() { return kodeOtp; }
    public void setKodeOtp(String kodeOtp) { this.kodeOtp = kodeOtp; }
    public LocalDateTime getWaktuDibuat() { return waktuDibuat; }
    public void setWaktuDibuat(LocalDateTime waktuDibuat) { this.waktuDibuat = waktuDibuat; }
    public LocalDateTime getWaktuKadaluarsa() { return waktuKadaluarsa; }
    public void setWaktuKadaluarsa(LocalDateTime waktuKadaluarsa) { this.waktuKadaluarsa = waktuKadaluarsa; }
    public short getJumlahSalah() { return jumlahSalah; }
    public void setJumlahSalah(short jumlahSalah) { this.jumlahSalah = jumlahSalah; }
    public String getStatusOtp() { return statusOtp; }
    public void setStatusOtp(String statusOtp) { this.statusOtp = statusOtp; }
}
