package com.chat.socket.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

//import javax.swing.text.html.HTMLDocument.HTMLReader.ParagraphAction;

import org.springframework.stereotype.Component;

import com.chat.model.ChatMessage;
import com.chat.model.ChatRoom;
import com.chat.model.LastSeen;
import com.chat.model.RoomType;
import com.chat.model.SocketEvent;
import com.chat.payloads.request.MessageRequest;
import com.chat.payloads.request.SocketMessageResponse;
import com.chat.payloads.request.StatusRequest;
import com.chat.payloads.response.ApiResponse;
import com.chat.payloads.response.MessageReply;
import com.chat.repositories.ChatMessageRepository;
import com.chat.repositories.ChatRoomRepository;
import com.chat.repositories.LastSeenRepository;
import com.chat.service.IChatRoomService;
import com.chat.service.impl.UserSessionManager;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SocketModule {

	private final SocketIOServer server;
	private final IChatRoomService chatRoomService;
	private final UserSessionManager userSessionManager;
	private final ChatMessageRepository chatMessageRepository;
	private final LastSeenRepository lastSeenRepository;
	private final ChatRoomRepository chatRoomRepository;

	public SocketModule(SocketIOServer server, IChatRoomService chatRoomService, UserSessionManager userSessionManager,
			ChatMessageRepository chatMessageRepository, LastSeenRepository lastSeenRepository,
			ChatRoomRepository chatRoomRepository) {
		this.server = server;
		this.chatRoomService = chatRoomService;
		this.userSessionManager = userSessionManager;
		this.chatMessageRepository = chatMessageRepository;
		this.lastSeenRepository = lastSeenRepository;
		this.chatRoomRepository = chatRoomRepository;
		server.addConnectListener(onConnected());
		server.addDisconnectListener(onDisconnected());

		// send messages to client
		server.addEventListener(SocketEvent.SEND_MESSAGE.toString(), MessageRequest.class, onChatReceived());

		// update status of sender
		server.addEventListener(SocketEvent.ON_STATUS_CHANGE.toString(), StatusRequest.class, onChangeStatus());
	}

	private DataListener<StatusRequest> onChangeStatus() {
		return (senderClient, data, ackSender) -> {
			SocketIOClient socketIOClient = fetchServerClient(data.getRecipientId());

			switch (data.getEventType()) {
			case TYPING:
				changeTypingStatus(data, SocketEvent.ON_STATUS_CHANGE.toString(), socketIOClient);
				break;
			case MESSAGE_SEEN_STATUS:
				sendStatusMessage(data, SocketEvent.ON_STATUS_CHANGE.toString(), socketIOClient);
				break;
			case DELETE_MESSAGE:
				deleteMessage(data, SocketEvent.ON_STATUS_CHANGE.toString(), socketIOClient);
				break;
			case SEEN_MESSAGES:
				seenAllUnseenMessages(data.getRecipientId(), data.getRoomId(), data.getSenderId());
			default:
				throw new IllegalArgumentException("Unexpected event value");
			}
		};
	}

	private void deleteMessage(StatusRequest data, String string, SocketIOClient socketIOClient) {

		// implements the delete message logic here
		ChatMessage chatMessage = chatMessageRepository.findById(data.getMessageId())
				.orElseThrow(() -> new RuntimeException(Constant.MESSAGE_NOT_FOUND));
		chatMessage.setDeleted(true);
		chatMessageRepository.save(chatMessage);

		// sending response via socket to receipeint insure message must be delete to
		// receiver
		Map<String, Object> response = new HashMap<>();
		response.put(Constant.EVENT_TYPE, SocketEvent.DELETE_MESSAGE.toString());
		response.put(Constant.ROOM_ID, data.getRoomId());
		response.put(Constant.MESSAGE_ID, data.getMessageId());
		// socketIOClient.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);

		chatMessage.getChatRoom().getParticipants().forEach(obj -> {
			UUID sessionIdByEmail = userSessionManager.getSessionIdByEmail(obj);
			SocketIOClient client = server.getClient(sessionIdByEmail);
			if (client != null) {
				client.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);
			}
		});
	}

	private void changeTypingStatus(StatusRequest data, String string, SocketIOClient socketIOClient) {
		Map<String, Object> response = new HashMap<>();
		response.put(Constant.EVENT_TYPE, SocketEvent.TYPING.toString());
		response.put(Constant.ROOM_ID, data.getRoomId());
		response.put(Constant.SENDER_ID, data.getSenderId());
		socketIOClient.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);

	}

	public void sendStatusMessage(StatusRequest data, String event, SocketIOClient recieverClien) {

		log.info(data.getBulkMessageSeen() ? "bulk changes" : "single change");
		Map<String, Object> response = new HashMap<>();
		response.put(Constant.ROOM_ID, data.getRoomId());
		response.put(Constant.IS_SEEN, true);
		response.put(Constant.BULK_MESSAGE_SEEN, data.getBulkMessageSeen());
		response.put(Constant.EVENT_TYPE, SocketEvent.MESSAGE_SEEN_STATUS);

		Optional<ChatMessage> message = null;
		if (data.getBulkMessageSeen()) {
			response.put("messageIds", data.getMessageIds());
		} else {
			// One_to_one chat message seen
			message = chatMessageRepository.findById(data.getMessageId());
			if (message.isPresent()) {
				message.get().getParticipants().add(data.getSenderId());
				chatMessageRepository.save(message.get());
			}
			response.put(Constant.MESSAGE_ID, data.getMessageId());

		}
		// Group message seen
		if (message != null && message.get().getChatRoom().getRoomType().equals(RoomType.GROUP)) {
			if (message.get().getChatRoom().getParticipants().size() == message.get().getParticipants().size() + 1) {
				recieverClien.sendEvent(event, response);
			}
		} else {
			recieverClien.sendEvent(event, response);
		}
	}

	private DataListener<MessageRequest> onChatReceived() {
		return (senderClient, data, ackSender) -> {
			try {
				// Save the message and get the response
				ApiResponse apiResponse = chatRoomService.saveMessage(data).getBody();
				ChatMessage messageObject = (ChatMessage) apiResponse.getData().get("message");
				String roomId = (String) apiResponse.getData().get("roomId");
				RoomType roomType = (RoomType) apiResponse.getData().get("roomType");
				// Create the message response
				SocketMessageResponse messageResponse = new SocketMessageResponse();
				messageResponse.setContent(messageObject.getContent());
				messageResponse.setRoomId(roomId);
				messageResponse.setMessageId(messageObject.getMessageId());
				messageResponse.setAttachments(messageObject.getAttachments());
				messageResponse.setSenderId(data.getSenderId());
				messageResponse.setLocalTime(LocalDateTime.now().toLocalTime().toString());
				messageResponse.setRoomType(roomType);
				messageResponse.setCreatedDate(messageObject.getCreatedDate().toLocalDate().toString());

				if (messageObject.getReplyMessageId() != null) {
					Optional<ChatMessage> message = chatMessageRepository.findById(messageObject.getReplyMessageId());
					MessageReply reply = MessageReply.builder().content(message.get().getContent())
							.messageId(message.get().getMessageId()).build();
					messageResponse.setMessageReply(reply);
				}
				// Send the message response
				sendMessage(messageResponse, "get_message", senderClient);

			} catch (Exception e) {
				e.printStackTrace();
			}
		};
	}

	private ConnectListener onConnected() {
		return (client) -> {
			String room = client.getHandshakeData().getSingleUrlParam("room");
			client.joinRoom(room);
			userSessionManager.addUserSession(room, client.getSessionId());
			log.info("Socket ID[{}]  Connected to socket", client.getSessionId().toString());

			// updating online status
			updateOnlineOfflineStatus(room, true);

			// updating single to double tick when user comes online or connect to socket
			// server
			changeSingleToDoubleTick(room);
		};
	}

	public void updateOnlineOfflineStatus(String roomId, Boolean isOnline) {

		Map<String, Object> response = new HashMap<>();
		response.put(Constant.IS_ONLINE, isOnline);
		response.put(Constant.LAST_SEEN, isOnline ? "" : LocalDateTime.now().toString());
		response.put(Constant.CONNECTED_CLIENT_ID, roomId);
		response.put(Constant.EVENT_TYPE, SocketEvent.ONLINE_OFFLINE_STATUS);

		UUID connectedClientId = userSessionManager.getSessionIdByEmail(roomId);

		List<UUID> allSession = userSessionManager.getAllSession();
		allSession.forEach(obj -> {
			SocketIOClient client = server.getClient(obj);
			if (client != null || connectedClientId != null && server.getClient(connectedClientId) != client) {
				client.sendEvent(SocketEvent.ONLINE_OFFLINE_STATUS.toString(), response);
			}
		});
	}

	private DisconnectListener onDisconnected() {
		return client -> {
			UUID sessionId = client.getSessionId();
			String email = userSessionManager.removeUserSession(sessionId);
			LastSeen lastSeen = LastSeen.builder().participantId(email).lastSeenTime(LocalDateTime.now()).build();
			Optional<LastSeen> participant = lastSeenRepository.findById(email);
			if (participant.isPresent()) {
				participant.get().setLastSeenTime(LocalDateTime.now());
				lastSeenRepository.save(participant.get());
			} else
				lastSeenRepository.save(lastSeen);

			// updating last seen of user
			// updating online status
			updateOnlineOfflineStatus(email, false);

			log.info("Client[{}] - Disconnected from socket", client.getSessionId().toString());
		};
	}

	public void sendMessage(SocketMessageResponse data, String eventName, SocketIOClient senderClient) {

		// fetching all the participants by room id
		List<String> allParticipants = chatRoomService.getAllParticipants(data.getRoomId());
		Integer isSentTOAll = 0;
		for (String participantId : allParticipants) {
			SocketIOClient recipientClient = fetchServerClient(participantId);
			if (recipientClient != null) {
				recipientClient.sendEvent(eventName, data);
				isSentTOAll = isSentTOAll + 1;
			}
		}

		Optional<ChatMessage> message = chatMessageRepository.findById(data.getMessageId());
		// change single tick to double tick in case of one to one chatting
		Map<String, Object> response = new HashMap<>();
		response.put(Constant.EVENT_TYPE, SocketEvent.UPDATE_DOUBLE_CLICK);
		if (data.getRoomType().equals(RoomType.ONE_TO_ONE)) {

			if (isSentTOAll > 1) {
				response.put(Constant.MESSAGE_ID, data.getMessageId());
				response.put(Constant.ROOM_ID, data.getRoomId());
				response.put(Constant.IS_RECIEVED, true);
				senderClient.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);
				if (message.isPresent())
					message.get().setIsRecieved(true);

			} else {
				if (message.isPresent())
					message.get().setIsRecieved(false);
			}
		} else if (data.getRoomType().equals(RoomType.GROUP)) {
			if (message.isPresent() && isSentTOAll > 1) {
				response.put(Constant.MESSAGE_ID, data.getMessageId());
				response.put(Constant.ROOM_ID, data.getRoomId());
				response.put(Constant.IS_RECIEVED, true);
				senderClient.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);
				message.get().setIsRecieved(true);
			} else {
				message.get().setIsRecieved(false);
			}
			chatMessageRepository.save(message.get());
		}
	}

	// fetching client or participants from stored sessions
	public SocketIOClient fetchServerClient(String email) {
		UUID recipientSessionId = userSessionManager.getSessionIdByEmail(email);
		if (recipientSessionId != null) {
			SocketIOClient recipientClient = server.getClient(recipientSessionId);
			if (recipientClient != null) {
				return recipientClient;
			}
		} else {
			log.warn("Recipient with email [{}] is offline", email);
		}
		return null;
	}

	// single to double message tick as user connect to socket
	//
	public void changeSingleToDoubleTick(String userEmailId) // roomId
	{
		// steps:

		// 1. find all room of user
		// 2. then check is value received in message is false if false then change to
		// true
		// 3. and all fetch all client and associated room

		// 1
		List<ChatRoom> allUserRooms = chatRoomRepository.findAllUserRooms(userEmailId);
		Set<String> userSessions = new HashSet<>();
		Map<String, Object> response = new HashMap<>();

		if (!allUserRooms.isEmpty()) {
			// 2.
			allUserRooms.forEach(obj -> {
				List<ChatMessage> list = obj.getMessageList().stream()
						.filter(message -> !message.getIsRecieved() && !message.getSenderId().equals(userEmailId))
						.toList();
				if (!list.isEmpty()) {
					// storing client email(roomId) for updating message double tick
					obj.getParticipants().removeIf(obj1 -> obj1.equals(userEmailId));
					userSessions.addAll(obj.getParticipants());
					chatMessageRepository.updateMessageSingleTickToDoubleTick(list);
				}
			});
		}

		response.put(Constant.EVENT_TYPE, SocketEvent.UPDATE_DOUBLE_CLICK);
//		response.put("roomIds", userSessions);
		response.put(Constant.SENDER_ID, userEmailId);
		response.put(Constant.PARTICIPANT_ID, userEmailId);

		// now updating to web
		userSessions.forEach(obj -> {

			SocketIOClient client = fetchServerClient(obj);
			if (client != null) {
				client.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);
			}
		});
	}

	// seen bulk messages for group and single chat and (send the ACK back to
	// sender ) at the time of user open the the chat application
	public void seenAllUnseenMessages(String recipientId, String roomId, String senderId) {
		List<ChatMessage> unSeenMessages = new ArrayList<>();
		// for group messages
		if (recipientId == null || recipientId == "")
			unSeenMessages = chatMessageRepository.findAllUnseenGroupMessages(roomId, senderId);
		else // for single chat
			unSeenMessages = chatMessageRepository.findAllUnseenMessages(recipientId, roomId, senderId);

		if (!unSeenMessages.isEmpty()) {
			List<String> messageIds = unSeenMessages.stream().map(message -> {
				message.getParticipants().add(senderId);
				return message.getMessageId();
			}).toList();

			chatMessageRepository.saveAll(unSeenMessages);

			if (recipientId != null && recipientId != "") {
				SocketIOClient socketIOClient = fetchServerClient(recipientId);
				if (socketIOClient != null) {
					StatusRequest request = new StatusRequest();
					request.setBulkMessageSeen(true);
					request.setMessageIds(messageIds);
					request.setRecipientId(recipientId);
					request.setSenderId(senderId);
					request.setRoomId(roomId);
					sendStatusMessage(request, SocketEvent.ON_STATUS_CHANGE.toString(), socketIOClient);
				}
			}

			if (recipientId == null || recipientId == "") {
				unSeenMessages.forEach(obj -> {
					if (obj.getChatRoom().getParticipants().size() == obj.getParticipants().size() + 1) {
						SocketIOClient socketIOClient = fetchServerClient(obj.getSenderId());
						if (socketIOClient != null) {
							Map<String, Object> response = new HashMap<>();
							response.put(Constant.ROOM_ID, roomId);
							response.put(Constant.IS_SEEN, true);
							response.put(Constant.BULK_MESSAGE_SEEN, false);
							response.put(Constant.EVENT_TYPE, SocketEvent.MESSAGE_SEEN_STATUS);
							response.put(Constant.MESSAGE_ID, obj.getMessageId());
							socketIOClient.sendEvent(SocketEvent.ON_STATUS_CHANGE.toString(), response);
						}
					}
				});
			}
		}
	}
}
