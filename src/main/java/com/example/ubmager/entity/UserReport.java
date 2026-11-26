package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "userreport")
public class UserReport {
    @Id
    @Column(name = "id_report", length = 10, nullable = false)
    private String idReport;

    @Column(name = "id_user", length = 10, nullable = false)
    private String idUser;

    @Column(name = "id_user_dilaporkan", length = 10, nullable = false)
    private String idUserDilaporkan;

    @Column(name = "alasan", columnDefinition = "TEXT", nullable = false)
    private String alasan;

    public String getIdReport() { return idReport; }
    public void setIdReport(String idReport) { this.idReport = idReport; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getIdUserDilaporkan() { return idUserDilaporkan; }
    public void setIdUserDilaporkan(String idUserDilaporkan) { this.idUserDilaporkan = idUserDilaporkan; }
    public String getAlasan() { return alasan; }
    public void setAlasan(String alasan) { this.alasan = alasan; }
}
