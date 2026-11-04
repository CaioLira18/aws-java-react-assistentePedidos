package io.github.caiolira18.foodassistant.assistant.llm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;


public class FakeLlmClient implements LlmClient {
    private final Deque<LlmResponse> script = new ArrayDeque<>();
    private final List<LlmRequest> received = new ArrayList<>();

    public FakeLlmClient enqueue(LlmResponse response) { script.add(response); return this; }

    public static LlmResponse text(String text) {
        return new LlmResponse(text, List.of(), new Usage(10, 5), LlmResponse.StopReason.END_TURN);
    }
    public static LlmResponse toolCall(String id, String name, String argsJson) {
        return new LlmResponse("", List.of(new ToolCall(id, name, argsJson)),
                new Usage(10, 5), LlmResponse.StopReason.TOOL_USE);
    }

    @Override public LlmResponse generate(LlmRequest request) {
        received.add(request);
        if (script.isEmpty()) throw new IllegalStateException("FakeLlmClient: script exhausted");
        return script.poll();
    }

    @Override public LlmResponse stream(LlmRequest request, Consumer<String> onToken) {
        LlmResponse r = generate(request);
        if (!r.text().isBlank()) onToken.accept(r.text());
        return r;
    }

    public List<LlmRequest> received() { return List.copyOf(received); }
}
