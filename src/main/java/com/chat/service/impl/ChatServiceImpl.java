package com.chat.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.chat.payloads.request.StatusRequest;
import com.chat.payloads.response.AgencyChatUsers;
import com.chat.payloads.response.ApiResponse;
import com.chat.payloads.response.ChatRoomList;
import com.chat.repositories.ChatMessageRepository;
import com.chat.repositories.ChatRoomRepository;
import com.chat.service.IChatService;
import com.chat.socket.config.SocketModule;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final SocketModule socketModule;
	private final SocketIOServer server;
	private final UserSessionManager userSessionManager;

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

	
	// not  in use
	@Override
	public ResponseEntity<ApiResponse> createGroupChatRoom(RoomRequest roomRequest) {

		Map<String, Object> map = new HashMap<>();
		if (roomRequest.getRoomType() != null && !roomRequest.getRoomType().equals(RoomType.GROUP)) {
			map.put("message", "invalid room type !");
			return new ResponseEntity<ApiResponse>(new ApiResponse("Correct room type!!", map), HttpStatus.BAD_REQUEST);
		}

		ChatRoom room = ChatRoom.builder().participants(roomRequest.getParticipants()).build();
		room.setCreatedDate(LocalDateTime.now());
		room.setUpdatedDate(LocalDateTime.now());
		room.setRoomType(roomRequest.getRoomType());
		ChatRoom save = chatRoomRepository.save(room);
		map.put("roomId", save.getRoomId());
		return new ResponseEntity<ApiResponse>(new ApiResponse("Created Room", map), HttpStatus.OK);
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

//			if (chatroom.getRoomType().equals(RoomType.ONE_TO_ONE)) {
//				chatroom.setUnSeenMessageCount(room.getMessageList().stream()
//						.filter(obj -> !obj.getSenderId().equals(userId) && !obj.getParticipants().contains(userId))
//						.count());
//			} else if (chatroom.getRoomType().equals(RoomType.ONE_TO_ONE)) {
				chatroom.setUnSeenMessageCount(room.getMessageList().stream()
						.filter(obj -> !obj.getSenderId().equals(userId) && !obj.getParticipants().contains(userId))
						.count());
		//	}

			return chatroom;
		}).collect(Collectors.toList());

		response.put("chatList", chatRooms);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

//	@Override
//	public ResponseEntity<?> getAllChatUsers(String userId) {
//
//		Map<String, Object> response = new HashMap<>();
//
//		List<AgencyChatUsers> agencyUser = new ArrayList<>();
//		agencyUser.add(new AgencyChatUsers("a", "kapilgmail.com", "", "Kapil Rathore"));
//		agencyUser.add(new AgencyChatUsers("b", "goutamgmail.com", "", "Goutam barfa"));
//		agencyUser.add(new AgencyChatUsers("c", "rahulgmail.com", "", "Rahul hencha"));
//		agencyUser.add(new AgencyChatUsers("d", "rohitgmail.com", "", "Rohit lodhi"));
//
//		List<ChatRoomList> chatRooms = new ArrayList<>();
//		List<AgencyChatUsers> participants = new ArrayList<>();
//		// database rooms
//		List<ChatRoom> totalRooms = chatRoomRepository.findAll();
//
//		// creating chatting room list response;
//		totalRooms.forEach(room -> {
//
//			ChatRoomList chatroom = new ChatRoomList();
//
//			if (room.getRoomType().equals(RoomType.ONE_TO_ONE)) {
//				// remove the current user from the participants in case of one to one chat
//				room.getParticipants().removeIf(item -> item.equals(userId));
//			}
//
//			room.getParticipants().forEach(participant -> {
//				agencyUser.forEach(pt -> {
//					if (pt.getEmail().equals(participants)) {
//						participants.add(pt);
//					}
//				});
//			});
//			Optional<ChatMessage> lastMessage = room.getMessageList().stream().filter(obj -> !obj.isSeen())
//					.sorted(Comparator.comparing(ChatMessage::getCreatedDate).reversed()).findFirst();
//
//			if (lastMessage.isPresent())
//				chatroom.setLastMessage(lastMessage.get().getContent());
//			else
//				chatroom.setLastMessage("");
//
//			chatroom.setRoomId(room.getRoomId());
//			chatroom.setRoomName(room.getRoomName());
//			chatroom.setRoomType(room.getRoomType());
//			chatroom.setUnSeenMessageCoun(room.getMessageList().stream().filter(obj -> !obj.isSeen()).count());
//			chatroom.setParticipants(participants);
//
//			// clearing the list of participants;
//			participants.clear();
//
//			chatRooms.add(chatroom);
//		});
//
//		response.put("chatList", chatRooms);
//
//		return new ResponseEntity<>(response, HttpStatus.OK);
//	}

	// seen bulk messages
	@Override
	public ResponseEntity<?> seenAllUnseenMessages(String recipientId, String roomId, String senderId) {
		List<ChatMessage> unSeenMessages = new ArrayList<>();
		// for group messages
		if (recipientId == null || recipientId == "") {
			unSeenMessages = chatMessageRepository.findAllUnseenGroupMessages(roomId, senderId);
		} else  // for single chat
		{
			unSeenMessages = chatMessageRepository.findAllUnseenMessages(recipientId, roomId, senderId);
		}

		if (!unSeenMessages.isEmpty()) {
			List<String> messageIds = unSeenMessages.stream().map(message -> {
				message.getParticipants().add(senderId);
				return message.getMessageId();
			}).toList();

			chatMessageRepository.saveAll(unSeenMessages);

			if (recipientId != null || recipientId != "") {
				SocketIOClient socketIOClient = socketModule.fetchServerClient(recipientId);
				if (socketIOClient != null) {
					StatusRequest request = new StatusRequest();
					request.setBulkMessageSeen(true);
					request.setMessageIds(messageIds);
					request.setRecipientId(recipientId);
					request.setSenderId(senderId);
					request.setRoomId(roomId);
					socketModule.sendStatusMessage(request, "onStatusChange", socketIOClient);
				}
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	// change single tick to double tick to all the user who send messages in case
	// of one to one chatting when user is goes offline to online
	public void ChangeAllMessageStatus(String senderId) {
		
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
