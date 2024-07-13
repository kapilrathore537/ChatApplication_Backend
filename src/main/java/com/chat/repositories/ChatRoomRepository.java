package com.chat.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chat.model.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

	@Query("SELECT ch FROM ChatRoom ch " + "JOIN ch.participants p " + "WHERE SIZE(ch.participants) = :sizeOfIds "
			+ "AND p IN :ids " + "GROUP BY ch " + "HAVING COUNT(p) = :sizeOfIds")
	ChatRoom existsByParticipants(@Param("ids") List<String> ids, @Param("sizeOfIds") long sizeOfIds);

//	@Query(value = "SELECT * FROM chat_room as ch "
//			+ "JOIN chat_room_participants as cp ON ch.room_id = cp.room_id "
//			+ "WHERE cp.participants =:senderId", nativeQuery = true)
//	List<ChatRoom> findAllUserRooms(@Param("senderId") String senderId);

	 @Query("SELECT cr FROM ChatRoom cr " +
	           "JOIN cr.participants p " +
	           "WHERE :senderId MEMBER OF cr.participants")
	    List<ChatRoom> findAllUserRooms(@Param("senderId") String senderId);
}
