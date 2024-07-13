package com.chat.payloads.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chat.model.Auditable;
import com.chat.model.RoomType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatRoomResponse extends Auditable {

	private String roomId;
	private String roomName;
//	private List<MessageResponse> messageList;
	private RoomType roomType;
	private List<AgencyChatUsers> participants = new ArrayList<>();
	Map<LocalDate, List<MessageResponse>> messageList = new HashMap<>();
	private boolean isOnline;
}
