package com.chat.payloads.request;

import java.util.ArrayList;
import java.util.List;

import com.chat.model.RoomType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomRequest {

	private List<String> participants = new ArrayList<>();
//	private String SenderId;
//	private String RecipientId;
	private RoomType roomType;
	private String roomId;
	private String roomName;
}
