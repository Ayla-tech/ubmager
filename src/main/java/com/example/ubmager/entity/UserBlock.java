package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "userblock")
public class UserBlock {
    @Id
    @Column(name = "id_block", length = 10, nullable = false)
    private String idBlock;

    @Column(name = "id_user", length = 10, nullable = false)
    private String idUser;

    @Column(name = "id_user_diblokir", length = 10, nullable = false)
    private String idUserDiblokir;

    @Column(name = "status_blokir", nullable = false)
    private boolean statusBlokir;

    public String getIdBlock() { return idBlock; }
    public void setIdBlock(String idBlock) { this.idBlock = idBlock; }
    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }
    public String getIdUserDiblokir() { return idUserDiblokir; }
    public void setIdUserDiblokir(String idUserDiblokir) { this.idUserDiblokir = idUserDiblokir; }
    public boolean isStatusBlokir() { return statusBlokir; }
    public void setStatusBlokir(boolean statusBlokir) { this.statusBlokir = statusBlokir; }
}
