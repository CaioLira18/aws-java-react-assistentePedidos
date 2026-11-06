package io.github.caiolira18.foodassistant.assistant.llm;

import java.util.function.Consumer;

public interface LlmClient {
    LlmResponse generate(LlmRequest request);
    LlmResponse stream(LlmRequest request, Consumer<String> onToken);
}
