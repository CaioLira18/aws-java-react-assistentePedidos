package io.github.caiolira18.foodassistant.assistant.llm;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.llm.LlmRequest;
import io.github.caiolira18.foodassistant.assistant.llm.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FakeLlmClientTest {

    @Test
    void returnsScriptedResponsesInOrder() {
        var fake = new FakeLlmClient()
            .enqueue(FakeLlmClient.toolCall("1", "search_dishes", "{}"))
            .enqueue(FakeLlmClient.text("ok"));
        var req = new LlmRequest("sys", List.of(Message.user("oi")), List.of(), 500);

        assertTrue(fake.generate(req).wantsTools());
        assertEquals("ok", fake.generate(req).text());
        assertThrows(IllegalStateException.class, () -> fake.generate(req));
    }
}
