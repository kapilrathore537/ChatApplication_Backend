package com.chat.service.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class UserSessionManager {

	public final ConcurrentHashMap<String, UUID> userSessions = new ConcurrentHashMap<>();

	public void addUserSession(String email, UUID sessionId) {
		userSessions.put(email, sessionId);
	}

	public String removeUserSession(UUID sessionId) {
		String removedKey = null;

		// Iterate over the entry set to find and remove the matching session
		Iterator<Map.Entry<String, UUID>> iterator = userSessions.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, UUID> entry = iterator.next();
			if (entry.getValue().equals(sessionId)) {
				removedKey = entry.getKey();
				iterator.remove();
				break; // Exit the loop once the session is found and removed
			}
		}
		return removedKey;
	}

	public UUID getSessionIdByEmail(String recipientId) {
		return recipientId == null ? null : userSessions.get(recipientId);
	}

	public List<UUID> getAllSession() {
		return Collections.list(userSessions.elements());
	}
}