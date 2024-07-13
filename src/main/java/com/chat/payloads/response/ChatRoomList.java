package com.chat.payloads.response;

import java.util.List;

import com.chat.model.RoomType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomList {
	private String roomId;
	private String roomName;
	private RoomType roomType;
	private List<AgencyChatUsers> participants;
	private Long unSeenMessageCount = 0l;
	private String lastMessage;
	private String lastMessageTime;
}
