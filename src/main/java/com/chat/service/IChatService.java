package com.chat.service;

import org.springframework.http.ResponseEntity;

import com.chat.payloads.request.RoomRequest;
import com.chat.payloads.response.ApiResponse;

public interface IChatService {

	public ResponseEntity<ApiResponse> createChatRoom(RoomRequest roomRequest);

	public ResponseEntity<ApiResponse> addUserToGroupChatRoom(RoomRequest roomReques);

	public ResponseEntity<?> getAllChatUsers(String userId);

}
