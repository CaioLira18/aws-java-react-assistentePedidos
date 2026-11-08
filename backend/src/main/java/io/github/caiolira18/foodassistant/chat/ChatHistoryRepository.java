package io.github.caiolira18.foodassistant.chat;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.github.caiolira18.foodassistant.assistant.llm.Message;

@Repository
class ChatHistoryRepository {

    private final JdbcTemplate jdbc;

    ChatHistoryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<Message> recent(String sessionId, int limit) {
        return jdbc.query("""
                SELECT role, content FROM (
                    SELECT id, role, content FROM chat_message
                    WHERE session_id = ? AND role IN ('user', 'assistant')
                    ORDER BY id DESC LIMIT ?
                ) recent ORDER BY id
                """, (rs, i) -> "user".equals(rs.getString("role"))
                        ? Message.user(rs.getString("content"))
                        : Message.assistant(rs.getString("content"), List.of()),
                sessionId, limit);
    }

    void appendTurn(String sessionId, String userText, String reply) {
        jdbc.update("""
                INSERT INTO chat_message (session_id, role, content)
                VALUES (?, 'user', ?), (?, 'assistant', ?)
                """, sessionId, userText, sessionId, reply);
    }
}