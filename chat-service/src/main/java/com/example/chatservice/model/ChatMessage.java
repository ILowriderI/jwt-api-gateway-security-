package com.example.chatservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class ChatMessage {
    @Id
    @GeneratedValue
    private Integer id;
    private String senderName;
    private String recipientName;
    private String body;

}
