package io.github.caiolira18.foodassistant.assistant.agent;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.caiolira18.foodassistant.assistant.llm.LlmClient;
import io.github.caiolira18.foodassistant.assistant.llm.LlmRequest;
import io.github.caiolira18.foodassistant.assistant.llm.LlmResponse;
import io.github.caiolira18.foodassistant.assistant.llm.Message;
import io.github.caiolira18.foodassistant.assistant.llm.ToolCall;
import io.github.caiolira18.foodassistant.assistant.llm.Usage;

public class AgentOrchestrator {

    static final String ITERATION_LIMIT_REPLY = "Não consegui concluir seu pedido agora. Pode reformular ou tentar de novo?";
    static final String TIMEOUT_REPLY = "Demorei mais do que o esperado. Pode tentar de novo em instantes?";
    static final String TOOL_LIMIT_ERROR = "Limite de chamadas de ferramenta por turno excedido";

    private final LlmClient llm;
    private final ToolRegistry tools;
    private final AgentSettings settings;
    private final Clock clock;

    public AgentOrchestrator(LlmClient llm, ToolRegistry tools, AgentSettings settings) {
        this(llm, tools, settings, Clock.systemUTC());
    }

    public AgentOrchestrator(LlmClient llm, ToolRegistry tools, AgentSettings settings, Clock clock) {
        this.llm = llm;
        this.tools = tools;
        this.settings = settings;
        this.clock = clock;
    }

    /** onToken nulo usa generate; informado, usa stream e repassa o texto conforme chega. */
    public AgentResult run(String sessionId, List<Message> history, String userText, Consumer<String> onToken) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId é obrigatório");
        }
        if (userText == null || userText.isBlank()) {
            throw new IllegalArgumentException("A mensagem não pode ser vazia");
        }

        ToolContext context = new ToolContext(sessionId);
        Instant start = clock.instant();
        Instant deadline = start.plus(settings.timeout());

        List<Message> conversation = new ArrayList<>(trimHistory(history));
        List<Message> produced = new ArrayList<>();
        append(conversation, produced, Message.user(userText));

        int inputTokens = 0;
        int outputTokens = 0;
        int iterations = 0;

        while (iterations < settings.maxIterations()) {
            if (!clock.instant().isBefore(deadline)) {
                return fallback(TIMEOUT_REPLY, AgentOutcome.TIMEOUT, iterations, inputTokens, outputTokens,
                        start, produced, onToken);
            }
            iterations++;

            LlmRequest request = new LlmRequest(AgentPrompt.SYSTEM, conversation, tools.definitions(),
                    settings.maxTokens());
            LlmResponse response = onToken == null ? llm.generate(request) : llm.stream(request, onToken);

            Usage usage = response.usage();
            if (usage != null) {
                inputTokens += usage.inputTokens();
                outputTokens += usage.outputTokens();
            }

            if (!response.wantsTools()) {
                append(conversation, produced, Message.assistant(response.text(), List.of()));
                AgentOutcome outcome = response.stopReason() == LlmResponse.StopReason.MAX_TOKENS
                        ? AgentOutcome.MAX_TOKENS
                        : AgentOutcome.COMPLETED;
                return finish(response.text(), outcome, iterations, inputTokens, outputTokens, start, produced);
            }

            List<ToolCall> calls = response.toolCalls();
            append(conversation, produced, Message.assistant(response.text(), calls));
            for (int i = 0; i < calls.size(); i++) {
                ToolCall call = calls.get(i);
                String result = i < settings.maxToolCallsPerTurn()
                        ? tools.execute(call, context)
                        : ToolRegistry.errorJson(TOOL_LIMIT_ERROR);
                append(conversation, produced, Message.toolResult(call.id(), result));
            }
        }

        return fallback(ITERATION_LIMIT_REPLY, AgentOutcome.ITERATION_LIMIT, iterations, inputTokens,
                outputTokens, start, produced, onToken);
    }

    /** Janela das últimas N mensagens, sempre começando numa mensagem do usuário (sem par tool_use/tool_result cortado). */
    List<Message> trimHistory(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, history.size() - settings.maxHistoryMessages());
        List<Message> window = history.subList(from, history.size());
        int start = 0;
        while (start < window.size() && window.get(start).role() != Message.Role.USER) {
            start++;
        }
        return List.copyOf(window.subList(start, window.size()));
    }

    private static void append(List<Message> conversation, List<Message> produced, Message message) {
        conversation.add(message);
        produced.add(message);
    }

    private AgentResult fallback(String reply, AgentOutcome outcome, int iterations, int inputTokens,
            int outputTokens, Instant start, List<Message> produced, Consumer<String> onToken) {
        produced.add(Message.assistant(reply, List.of()));
        if (onToken != null) {
            onToken.accept(reply);
        }
        return finish(reply, outcome, iterations, inputTokens, outputTokens, start, produced);
    }

    private AgentResult finish(String reply, AgentOutcome outcome, int iterations, int inputTokens,
            int outputTokens, Instant start, List<Message> produced) {
        return new AgentResult(reply, outcome, iterations, new Usage(inputTokens, outputTokens),
                Duration.between(start, clock.instant()), produced);
    }
}