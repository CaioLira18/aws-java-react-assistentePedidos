package io.github.caiolira18.foodassistant.assistant.agent;

import java.time.Duration;
import java.util.List;

import io.github.caiolira18.foodassistant.assistant.llm.Message;
import io.github.caiolira18.foodassistant.assistant.llm.Usage;

/** newMessages: tudo que este turno produziu (para persistir), da mensagem do usuário à resposta final. */
public record AgentResult(String reply, AgentOutcome outcome, int iterations, Usage usage,
        Duration elapsed, List<Message> newMessages) {

    public AgentResult {
        newMessages = List.copyOf(newMessages);
    }
}