package io.github.caiolira18.foodassistant.shared;

import java.util.regex.Pattern;

public final class SessionIds {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    private SessionIds() {
    }

    public static String validate(String sessionId) {
        if (sessionId == null || !PATTERN.matcher(sessionId).matches()) {
            throw new IllegalArgumentException("Sessão inválida");
        }
        return sessionId;
    }
}