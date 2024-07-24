package com.chat.service.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chat.model.ChatMessage;
import com.chat.model.ChatRoom;
import com.chat.model.RoomType;
import com.chat.payloads.request.RoomRequest;
import com.chat.payloads.response.AgencyChatUsers;
import com.chat.payloads.response.ApiResponse;
import com.chat.payloads.response.ChatRoomList;
import com.chat.repositories.ChatRoomRepository;
import com.chat.service.IChatService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

	private final ChatRoomRepository chatRoomRepository;

	public static final List<AgencyChatUsers> agencyUser = List.of(
			new AgencyChatUsers("a", "kapilgmail.com", "", "Kapil Rathore"),
			new AgencyChatUsers("b", "goutamgmail.com", "", "Goutam Barfa"),
			new AgencyChatUsers("c", "rahulgmail.com", "", "Rahul Hencha"),
			new AgencyChatUsers("d", "rohitgmail.com", "", "Rohit Lodhi"));

	@Override
	public ResponseEntity<ApiResponse> createChatRoom(RoomRequest roomRequest) {

		Map<String, Object> map = new HashMap<>();
		if (roomRequest.getRoomType().equals(RoomType.ONE_TO_ONE)) {
			Optional<ChatRoom> chat = chatRoomRepository.findAll().stream().filter(obj -> {
				List<String> participants = obj.getParticipants();
				if (participants.containsAll(roomRequest.getParticipants())
						&& obj.getRoomType().equals(RoomType.ONE_TO_ONE))
					return true;
				else
					return false;
			}).findFirst();

			if (chat.isPresent()) {
				map.put("roomId", chat.get().getRoomId());
				return new ResponseEntity<ApiResponse>(new ApiResponse("Already Created", map), HttpStatus.OK);
			}
		}

		ChatRoom room = ChatRoom.builder().participants(roomRequest.getParticipants()).build();
		room.setCreatedDate(LocalDateTime.now());
		room.setUpdatedDate(LocalDateTime.now());
		room.setRoomName(roomRequest.getRoomType().equals(RoomType.GROUP) ? roomRequest.getRoomName()
				: roomRequest.getRoomName() != null ? roomRequest.getRoomName() : "Group");
		room.setRoomType(roomRequest.getRoomType());
		ChatRoom save = chatRoomRepository.save(room);
		map.put("roomId", save.getRoomId());
		return new ResponseEntity<ApiResponse>(
				new ApiResponse(roomRequest.getRoomType().equals(RoomType.ONE_TO_ONE) ? "ONE_TO_ONE Room Created"
						: "GRUOP Room Created", map),
				HttpStatus.OK);
	}

	@Override
	public ResponseEntity<ApiResponse> addUserToGroupChatRoom(RoomRequest roomRequest) {
		Map<String, Object> map = new HashMap<>();

		Optional<ChatRoom> room = chatRoomRepository.findById(roomRequest.getRoomId());
		if (room.isPresent()) {
			room.get().getParticipants().addAll(roomRequest.getParticipants());
			return new ResponseEntity<ApiResponse>(new ApiResponse("participant add successfully", map),
					HttpStatus.CREATED);
		} else {
			return new ResponseEntity<ApiResponse>(new ApiResponse("room not found", map), HttpStatus.BAD_REQUEST);
		}

	}

	@Override
	public ResponseEntity<?> getAllChatUsers(String userId) {
		Map<String, Object> response = new HashMap<>();

		Map<String, AgencyChatUsers> emailToAgencyUserMap = agencyUser.stream()
				.collect(Collectors.toMap(AgencyChatUsers::getEmail, Function.identity()));

		List<ChatRoom> totalRooms = chatRoomRepository.findAllUserRooms(userId);
		// Filter rooms where the current user is not in the participants list
		List<ChatRoomList> chatRooms = totalRooms.stream().map(room -> {
			ChatRoomList chatroom = new ChatRoomList();

			if (room.getRoomType().equals(RoomType.ONE_TO_ONE)) {
				room.getParticipants().removeIf(id -> id.equals(userId));
			}

			List<AgencyChatUsers> participants = room.getParticipants().stream().map(emailToAgencyUserMap::get)
					.filter(Objects::nonNull).collect(Collectors.toList());

			chatroom.setParticipants(participants);

			Optional<ChatMessage> lastMessage = room.getMessageList().stream()
					.filter(obj -> !obj.getSenderId().equals(userId))
					.max(Comparator.comparing(ChatMessage::getCreatedDate));

			chatroom.setLastMessage(lastMessage.map(ChatMessage::getContent).orElse(""));
			chatroom.setRoomId(room.getRoomId());
			chatroom.setRoomName(room.getRoomName());
			chatroom.setRoomType(room.getRoomType());
			if (lastMessage.isPresent()) {
				chatroom.setLastMessageTime(lastMessage.get().getCreatedDate().toLocalTime().toString());
			} else {
				chatroom.setLastMessageTime("");
			}
			chatroom.setUnSeenMessageCount(room.getMessageList().stream()
					.filter(obj -> !obj.getSenderId().equals(userId) && !obj.getParticipants().contains(userId))
					.count());
			chatroom.setUnSeenMessageCount(room.getMessageList().stream()
					.filter(obj -> !obj.getSenderId().equals(userId) && !obj.getParticipants().contains(userId))
					.count());
			return chatroom;
		}).collect(Collectors.toList());

		response.put("chatList", chatRooms);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public static List<AgencyChatUsers> agencyUserToResponse() {
		return agencyUser.stream().map(obj -> {
			AgencyChatUsers user = new AgencyChatUsers();
			user.setEmail(obj.getEmail());
			user.setProfilePic(obj.getProfilePic());
			user.setUserId(obj.getUserId());
			user.setUserName(obj.getUserName());
			return user;
		}).toList();
	}

}
