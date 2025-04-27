package com.example.chatapp.repository;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    List<ChatRoom> findByIsPrivateFalse();

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p WHERE p = ?1")
    List<ChatRoom> findByParticipant(User user);

    List<ChatRoom> findByCreator(User creator);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.name LIKE %?1% OR cr.description LIKE %?1%")
    List<ChatRoom> searchRooms(String keyword);
}