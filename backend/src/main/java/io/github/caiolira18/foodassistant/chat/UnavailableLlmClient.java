package io.github.caiolira18.foodassistant.chat;

import java.util.function.Consumer;

import io.github.caiolira18.foodassistant.assistant.llm.LlmClient;
import io.github.caiolira18.foodassistant.assistant.llm.LlmRequest;
import io.github.caiolira18.foodassistant.assistant.llm.LlmResponse;

/** Usado enquanto não existe um LlmClient real: toda chamada falha. */
class UnavailableLlmClient implements LlmClient {

    @Override
    public LlmResponse generate(LlmRequest request) {
        throw new IllegalStateException("Nenhum LlmClient configurado");
    }

    @Override
    public LlmResponse stream(LlmRequest request, Consumer<String> onToken) {
        throw new IllegalStateException("Nenhum LlmClient configurado");
    }
}