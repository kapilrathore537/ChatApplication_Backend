package com.chat.payloads.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageRequest {

	private String roomId;
	private String senderId;
//	private String recipientId;
	private String content;
	private List<String> attachments;
	// private List<String> participants= new ArrayList<>();// List of user IDs
}
