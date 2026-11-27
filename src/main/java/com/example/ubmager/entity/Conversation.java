package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation")
public class Conversation {
    @Id
    @Column(name = "id_conversation", length = 10, nullable = false)
    private String idConversation;

    @Column(name = "id_user_pengirim", length = 10, nullable = false)
    private String idUserPengirim;

    @Column(name = "id_user_tujuan", length = 10, nullable = false)
    private String idUserTujuan;

    @Column(name = "id_post", length = 10, nullable = false)
    private String idPost;

    public String getIdConversation() { return idConversation; }
    public void setIdConversation(String idConversation) { this.idConversation = idConversation; }
    public String getIdUserPengirim() { return idUserPengirim; }
    public void setIdUserPengirim(String idUserPengirim) { this.idUserPengirim = idUserPengirim; }
    public String getIdUserTujuan() { return idUserTujuan; }
    public void setIdUserTujuan(String idUserTujuan) { this.idUserTujuan = idUserTujuan; }
    public String getIdPost() { return idPost; }
    public void setIdPost(String idPost) { this.idPost = idPost; }
}
