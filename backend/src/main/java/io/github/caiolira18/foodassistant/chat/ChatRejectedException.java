package io.github.caiolira18.foodassistant.chat;

import org.springframework.http.HttpStatus;

class ChatRejectedException extends RuntimeException {

    private final HttpStatus status;

    ChatRejectedException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    HttpStatus status() {
        return status;
    }
}