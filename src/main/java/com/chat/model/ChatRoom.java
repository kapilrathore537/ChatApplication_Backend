package com.chat.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(callSuper = false)
public class ChatRoom extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String roomId;
	private String roomName;

	@OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
	private List<ChatMessage> messageList = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	private RoomType roomType; // e.g., "one-to-one", "group"

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "chat_room_participants", joinColumns = @JoinColumn(name = "room_id"))
	private List<String> participants = new ArrayList<>();


}
