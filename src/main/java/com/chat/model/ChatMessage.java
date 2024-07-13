package com.chat.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "chatRoom")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(callSuper = false)
public class ChatMessage extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String messageId;

	@ManyToOne
	@JoinColumn(name = "room_id")
	private ChatRoom chatRoom;
	private String senderId;
	private String messageType;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable
	private List<String> attachments;
	@Column(columnDefinition = "longtext")
	private String content;

	@Builder.Default
	private Boolean edited = Boolean.FALSE;

	@Builder.Default
	private boolean deleted = Boolean.FALSE;
	
	@OneToOne
	private MessageReply reply;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "message_seen_participants", joinColumns = @JoinColumn(name = "message_id"))
	private List<String> participants = new ArrayList<>();
	@Builder.Default
	private Boolean isRecieved = Boolean.FALSE;

	public ChatMessage(String content, String messageId, String senderId) {
		this.content = content;
		this.messageId = messageId;
		this.senderId = senderId;
	}

}
