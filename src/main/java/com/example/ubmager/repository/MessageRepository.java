package com.example.ubmager.repository;

import com.example.ubmager.entity.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByIdConversationOrderByWaktuKirimAsc(String idConversation);
    Optional<Message> findFirstByIdConversationOrderByWaktuKirimDesc(String idConversation);
}
