package io.github.caiolira18.foodassistant.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import io.github.caiolira18.foodassistant.TestPostgres;
import io.github.caiolira18.foodassistant.assistant.llm.Message;

class ChatHistoryRepositoryTest {

    private static ChatHistoryRepository repository;

    @BeforeAll
    static void setUp() {
        String adminUrl = TestPostgres.jdbcUrl();
        new JdbcTemplate(new DriverManagerDataSource(adminUrl, "test", "test")).execute("CREATE DATABASE chat_test");

        var dataSource = new DriverManagerDataSource(
                adminUrl.substring(0, adminUrl.lastIndexOf('/')) + "/chat_test", "test", "test");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        repository = new ChatHistoryRepository(new JdbcTemplate(dataSource));
    }

    private static String session() {
        return UUID.randomUUID().toString();
    }

    @Test
    void appendedTurnsComeBackInOrderWithRoles() {
        String session = session();
        repository.appendTurn(session, "oi", "olá");
        repository.appendTurn(session, "quero feijoada", "claro");

        List<Message> messages = repository.recent(session, 20);

        assertEquals(List.of("oi", "olá", "quero feijoada", "claro"), messages.stream().map(Message::text).toList());
        assertEquals(Message.Role.USER, messages.get(0).role());
        assertEquals(Message.Role.ASSISTANT, messages.get(1).role());
    }

    @Test
    void limitKeepsTheMostRecentMessagesInChronologicalOrder() {
        String session = session();
        repository.appendTurn(session, "u1", "a1");
        repository.appendTurn(session, "u2", "a2");
        repository.appendTurn(session, "u3", "a3");

        List<Message> messages = repository.recent(session, 4);

        assertEquals(List.of("u2", "a2", "u3", "a3"), messages.stream().map(Message::text).toList());
    }

    @Test
    void historyIsIsolatedBySession() {
        String a = session();
        String b = session();
        repository.appendTurn(a, "oi", "olá");

        assertTrue(repository.recent(b, 20).isEmpty());
    }
}