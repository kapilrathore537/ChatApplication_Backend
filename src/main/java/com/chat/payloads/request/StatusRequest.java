package com.chat.payloads.request;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.chat.model.SocketEvent;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class StatusRequest {

	private String senderId;
	private String recipientId;
	private Boolean onLineStatus;
	private LocalDateTime lastSeen;
	private String roomId;
	private boolean isRecieved;
	private List<String> messageIds = new ArrayList<>();
	private String messageId;
	private boolean isSeen;
	private Boolean bulkMessageSeen;
	@Enumerated(EnumType.STRING)
	private SocketEvent eventType;
	
	
}
