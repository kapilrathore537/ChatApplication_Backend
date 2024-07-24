package com.chat.payloads.response;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.chat.model.MessageType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageResponse {

	private String messageId;
	private MessageType messageType;
	private String senderId;
	private List<String> attachments;
	private String content;
	private LocalTime localTime;
	private Boolean isSeen;
	private Boolean isRecieved;
	private LocalDateTime createdDate;
	private MessageReply messageReply;

}
