package com.example.ubmager.repository;

import com.example.ubmager.entity.Conversation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
    // Cari percakapan dua pengguna pada satu postingan, dari arah mana pun
    @Query("SELECT c FROM Conversation c WHERE c.idPost = :idPost AND "
            + "((c.idUserPengirim = :idUser AND c.idUserTujuan = :idTujuan) OR "
            + "(c.idUserPengirim = :idTujuan AND c.idUserTujuan = :idUser))")
    Optional<Conversation> cariConversation(@Param("idUser") String idUser,
            @Param("idTujuan") String idTujuan, @Param("idPost") String idPost);

    // Semua percakapan yang melibatkan pengguna
    @Query("SELECT c FROM Conversation c WHERE c.idUserPengirim = :idUser OR c.idUserTujuan = :idUser")
    List<Conversation> cariPercakapanUser(@Param("idUser") String idUser);
}
