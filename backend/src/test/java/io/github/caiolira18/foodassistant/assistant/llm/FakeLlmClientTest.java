package io.github.caiolira18.foodassistant.assistant.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class FakeLlmClientTest {

    private static LlmRequest request() {
        return new LlmRequest("sys", List.of(Message.user("oi")), List.of(), 500);
    }

    @Test
    void returnsScriptedResponsesInOrder() {
        var fake = new FakeLlmClient()
            .enqueue(FakeLlmClient.toolCall("1", "search_dishes", "{}"))
            .enqueue(FakeLlmClient.text("ok"));
        var req = request();

        assertTrue(fake.generate(req).wantsTools());
        assertEquals("ok", fake.generate(req).text());
        assertThrows(IllegalStateException.class, () -> fake.generate(req));
    }

    @Test
    void streamEmitsTextAndReturnsTheResponse() {
        var fake = new FakeLlmClient().enqueue(FakeLlmClient.text("Olá!"));
        List<String> tokens = new ArrayList<>();

        LlmResponse response = fake.stream(request(), tokens::add);

        assertEquals(List.of("Olá!"), tokens);
        assertEquals("Olá!", response.text());
    }

    @Test
    void streamEmitsNoTokensForToolCalls() {
        var fake = new FakeLlmClient().enqueue(FakeLlmClient.toolCall("1", "search_dishes", "{}"));
        List<String> tokens = new ArrayList<>();

        fake.stream(request(), tokens::add);

        assertTrue(tokens.isEmpty());
    }

    @Test
    void recordsReceivedRequests() {
        var fake = new FakeLlmClient().enqueue(FakeLlmClient.text("ok"));
        LlmRequest req = request();

        fake.generate(req);

        assertEquals(List.of(req), fake.received());
    }

    @Test
    void requestRejectsEmptyMessagesAndInvalidMaxTokens() {
        assertThrows(IllegalArgumentException.class, () -> new LlmRequest("", List.of(), List.of(), 500));
        assertThrows(IllegalArgumentException.class,
                () -> new LlmRequest("", List.of(Message.user("oi")), List.of(), 0));
    }

    @Test
    void toolResultMessageCarriesTheCallId() {
        Message result = Message.toolResult("1", "{\"ok\":true}");

        assertEquals(Message.Role.TOOL, result.role());
        assertEquals("1", result.toolCallId());
    }
}