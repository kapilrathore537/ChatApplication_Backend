package com.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "MessageReply")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(callSuper = false)
public class MessageReply extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String replyId;

	@OneToOne
	@JoinColumn(name = "messageId")
	private ChatMessage chatMessage;

	@Column(columnDefinition = "longtext")
	private String content;

	@Builder.Default
	private Boolean edited = Boolean.FALSE;

	@Builder.Default
	private boolean deleted = Boolean.FALSE;

	@Builder.Default
	private Boolean isRecieved = Boolean.FALSE;
}
