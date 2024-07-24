package com.chat.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.chat.payloads.request.MessageRequest;
import com.chat.payloads.response.ApiResponse;

public interface IChatRoomService {

	public ResponseEntity<ApiResponse> saveMessage(MessageRequest messageRequest);

	public List<String> getAllParticipants(String roomId);

	public ResponseEntity<ApiResponse> getAllMessages(String senderId, String roomId);

//	public Map<String, Object> createReply(MessageRequest messageRequest);

}
