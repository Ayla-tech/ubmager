package com.example.ubmager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @Column(name = "id_message", length = 10, nullable = false)
    private String idMessage;

    @Column(name = "id_conversation", length = 10, nullable = false)
    private String idConversation;

    @Column(name = "id_pengirim", length = 10, nullable = false)
    private String idPengirim;

    @Column(name = "isi_teks", columnDefinition = "TEXT")
    private String isiTeks;

    @Column(name = "file_gambar", length = 255)
    private String fileGambar;

    @Column(name = "waktu_kirim", nullable = false)
    private LocalDateTime waktuKirim;

    public String getIdMessage() { return idMessage; }
    public void setIdMessage(String idMessage) { this.idMessage = idMessage; }
    public String getIdConversation() { return idConversation; }
    public void setIdConversation(String idConversation) { this.idConversation = idConversation; }
    public String getIdPengirim() { return idPengirim; }
    public void setIdPengirim(String idPengirim) { this.idPengirim = idPengirim; }
    public String getIsiTeks() { return isiTeks; }
    public void setIsiTeks(String isiTeks) { this.isiTeks = isiTeks; }
    public String getFileGambar() { return fileGambar; }
    public void setFileGambar(String fileGambar) { this.fileGambar = fileGambar; }
    public LocalDateTime getWaktuKirim() { return waktuKirim; }
    public void setWaktuKirim(LocalDateTime waktuKirim) { this.waktuKirim = waktuKirim; }
}
