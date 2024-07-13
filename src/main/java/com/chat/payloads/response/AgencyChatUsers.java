package com.chat.payloads.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude(value = Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class AgencyChatUsers {
	private String userId;
	private String email;
	private String profilePic;
	private String userName;
	private Boolean isOnline;
	private String lastSeen;
	public AgencyChatUsers(String userId, String email, String profilePic, String userName) {
		super();
		this.userId = userId;
		this.email = email;
		this.profilePic = profilePic;
		this.userName = userName;
	}
	
	
}
