package com.chat.payloads.request;

import java.util.List;

import com.chat.model.RoomType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SocketMessageResponse {
	private String senderId;
	private String messageId;
	private String roomId;
	private String content;
	private List<String> attachments;
	private String localTime;
	private RoomType roomType;
	private String createdDate;

	public SocketMessageResponse(String senderId, String messageId, String roomId, String content,
			List<String> attachments, String localTime) {
		super();
		this.senderId = senderId;
		this.messageId = messageId;
		this.roomId = roomId;
		this.content = content;
		this.attachments = attachments;
		this.localTime = localTime;
	}

}
