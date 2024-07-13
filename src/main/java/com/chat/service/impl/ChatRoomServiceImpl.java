package com.chat.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.chat.model.ChatMessage;
import com.chat.model.ChatRoom;
import com.chat.model.LastSeen;
import com.chat.model.RoomType;
import com.chat.payloads.request.MessageRequest;
import com.chat.payloads.response.AgencyChatUsers;
import com.chat.payloads.response.ApiResponse;
import com.chat.payloads.response.ChatRoomResponse;
import com.chat.payloads.response.MessageResponse;
import com.chat.repositories.ChatMessageRepository;
import com.chat.repositories.ChatRoomRepository;
import com.chat.repositories.LastSeenRepository;
import com.chat.service.IChatRoomService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements IChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository messageRepository;
	private final UserSessionManager sessionManager;
	private final LastSeenRepository lastSeenRepository;

	@Override
	public ResponseEntity<ApiResponse> getAllMessages(String senderId, String roomId) {
		ChatRoom conversationRoom = getChatRoom(roomId);
		Map<String, Object> map = new HashMap<>();
		map.put("messages", ChatRoomToChatRoomResponse(conversationRoom, senderId));
		ApiResponse apiResponse = ApiResponse.builder().message("Fetched").data(map).build();

		return new ResponseEntity<ApiResponse>(apiResponse, HttpStatus.OK);
	}

	private ChatRoomResponse ChatRoomToChatRoomResponse(ChatRoom chatRoom, String senderId) {

		// fetch participant id from chatroom
		chatRoom.getParticipants().removeIf(obj -> obj.equals(senderId));

		List<ChatMessage> res = chatRoom.getMessageList().stream()
				.sorted(Comparator.comparing(ChatMessage::getCreatedDate).reversed()).collect(Collectors.toList());
		Map<LocalDate, List<MessageResponse>> collect = res.stream().collect(Collectors.groupingBy(
				message -> message.getCreatedDate().toLocalDate(), // Group by date
				Collectors.mapping(
						message -> messageToMessageResponse(message, senderId, chatRoom.getParticipants().get(0)),
						Collectors.collectingAndThen(Collectors.toList(),
								list -> list.stream().sorted(Comparator.comparing(MessageResponse::getCreatedDate)) // Sort
																													// MessageResponse
																													// by
										// createdDate
										.collect(Collectors.toList())))));

		// check user online
		return ChatRoomResponse.builder().roomId(chatRoom.getRoomId()).roomName(chatRoom.getRoomName())
				.roomType(chatRoom.getRoomType()).messageList(collect)
				.participants(agencyChatUsers(chatRoom.getParticipants(), senderId)).build();

	}

	private MessageResponse messageToMessageResponse(ChatMessage chatMessage, String senderId, String participantId) {
		return MessageResponse.builder().messageId(chatMessage.getMessageId()).senderId(chatMessage.getSenderId())
				.localTime(chatMessage.getCreatedDate().toLocalTime()).content(chatMessage.getContent())
				.attachments(chatMessage.getAttachments())
//				.isSeen(chatMessage.getChatRoom().getRoomType().equals(RoomType.ONE_TO_ONE)
//						? chatMessage.getSenderId().equals(senderId)
//								&& chatMessage.getParticipants().contains(participantId)
//						: false)
				.isSeen(checkIseenOrNot(chatMessage, senderId, participantId)).createdDate(chatMessage.getCreatedDate())
				.isRecieved(chatMessage.getIsRecieved()).build();
	}

	public Boolean checkIseenOrNot(ChatMessage chatMessage, String senderId, String participantId) {

		// if receiver seen message
		if (chatMessage.getChatRoom().getRoomType().equals(RoomType.ONE_TO_ONE)) {
			if (chatMessage.getSenderId().equals(senderId) && chatMessage.getParticipants().contains(participantId)) {
				return true;
			}
		} // if all the participants seen message
		else if (chatMessage.getChatRoom().getRoomType().equals(RoomType.GROUP)) {
			if (chatMessage.getSenderId().equals(senderId)
					&& chatMessage.getParticipants().size() == chatMessage.getChatRoom().getParticipants().size()) {
				return true;
			}
		}
		return false;
	}

	private List<AgencyChatUsers> agencyChatUsers(List<String> participantsIds, String senderId) {
		return ChatServiceImpl.agencyUser.stream().filter(obj -> {
			if (participantsIds.contains(obj.getEmail()) && !obj.getEmail().equals(senderId)) {

				UUID sessionIdByEmail = sessionManager.getSessionIdByEmail(obj.getEmail());
				if (sessionIdByEmail != null) {
					obj.setIsOnline(true);
				} else {
					obj.setIsOnline(false);
					Optional<LastSeen> participant = lastSeenRepository.findById(obj.getEmail());
					if (participant.isPresent()) {
						obj.setLastSeen(participant.get().getLastSeenTime().toString());
					}
				}
				return true;
			} else
				return false;

		}).toList();
	}

	@Override
	public ResponseEntity<ApiResponse> saveMessage(MessageRequest messageRequest) {

		ChatRoom room = getChatRoom(messageRequest.getRoomId());
					

		ChatMessage message = ChatMessage.builder().content(messageRequest.getContent())
				.attachments(messageRequest.getAttachments()).chatRoom(room).build();
		message.setCreatedDate(LocalDateTime.now());
		message.setUpdatedDate(LocalDateTime.now());
		message.setSenderId(messageRequest.getSenderId());
		message.setIsRecieved(false);

		ChatMessage save = messageRepository.save(message);
		room.getMessageList().add(save);
		chatRoomRepository.save(room);
		Map<String, Object> map = new HashMap<>();

		map.put("message", save);
		map.put("roomId", room.getRoomId());
		map.put("roomType", room.getRoomType());
		ApiResponse response = ApiResponse.builder().message("message saved").data(map).build();
		return new ResponseEntity<ApiResponse>(response, HttpStatus.CREATED);
	}

	private ChatRoom getChatRoom(String roomId) {
		return chatRoomRepository.findById(roomId).orElseThrow(() -> new RuntimeException("Room Not Found"));
	}

	@Override
	public List<String> getAllParticipants(String roomId) {
		ChatRoom chatRoom = getChatRoom(roomId);
		return chatRoom.getParticipants();
	}

}
