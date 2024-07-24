package com.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chat.payloads.request.RoomRequest;
import com.chat.payloads.response.ApiResponse;
import com.chat.service.IChatRoomService;
import com.chat.service.IChatService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin("*")
@RequestMapping(value = "/agency/chat")
@RequiredArgsConstructor
public class ChatController {

	private final IChatService chatService;
	private final IChatRoomService chatRoomService;

	// for one_to_one chatting
	@PostMapping(value = "/createRoom")
	public ResponseEntity<ApiResponse> createChatRoom(@RequestBody RoomRequest roomRequest) {
		return chatService.createChatRoom(roomRequest);
	}

	@GetMapping()
	public ResponseEntity<ApiResponse> getAllMessage(@RequestParam("senderId") String senderId,
			@RequestParam String roomId) {
		return chatRoomService.getAllMessages(senderId, roomId);
	}

	// add new user to existing group
	@PostMapping(value = "/addGroupUser")
	public ResponseEntity<ApiResponse> addUserToGroupChatRoom(@RequestBody RoomRequest roomReques) {
		return chatService.addUserToGroupChatRoom(roomReques);
	}

	@GetMapping("/getChatUsers")
	public ResponseEntity<?> getChatUsers(@RequestParam("userId") String userId) {
		return chatService.getAllChatUsers(userId);
	}

}
