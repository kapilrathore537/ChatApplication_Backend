package com.chat.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chat.model.ChatMessage;

import jakarta.transaction.Transactional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
	@Query("    SELECT ch FROM ChatMessage as ch "
			+ " WHERE ch.chatRoom.roomId =:roomId AND"
			+ " ch.senderId =:recipientId"
			+ " AND :senderId NOT MEMBER OF ch.participants ")
	List<ChatMessage> findAllUnseenMessages(@Param("recipientId") String recipientId, @Param("roomId") String roomId,@Param("senderId")String senderId);


	@Query("    SELECT ch FROM ChatMessage as ch "
			+ " WHERE ch.chatRoom.roomId =:roomId AND"
			+ " ch.senderId !=:senderId"
			+ " AND :senderId NOT MEMBER OF ch.participants ")
	List<ChatMessage> findAllUnseenGroupMessages( @Param("roomId") String roomId,@Param("senderId")String senderId);


	@Transactional
	@Modifying
	@Query("UPDATE ChatMessage as ch SET ch.isRecieved = TRUE WHERE ch IN :list")
	void updateMessageSingleTickToDoubleTick(@Param("list") List<ChatMessage> list);
		
}
