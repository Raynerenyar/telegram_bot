package zzb.telegram.bot.repository.entities;

import org.springframework.lang.NonNull;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "CHATS")
public class Chat {

    @Id
    @NonNull
    @Column(name = "CHAT_ID")
    private String chatId;

    @Positive
    @Column(name = "ALERT_VALUE")
    private long alertValue;

    @Column(name = "NEXT_DRAW_RECEIVED")
    @NonNull
    private boolean nextDrawReceived = false;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public long getAlertValue() {
        return alertValue;
    }

    public void setAlertValue(long alertValue) {
        this.alertValue = alertValue;
    }

    public boolean isNextDrawReceived() {
        return nextDrawReceived;
    }

    public void setNextDrawReceived(boolean nextDrawReceived) {
        this.nextDrawReceived = nextDrawReceived;
    }

}