package io.github.caiolira18.foodassistant.assistant.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.llm.ToolCall;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;

class ToolRegistryTest {

    private static final ToolContext CONTEXT = new ToolContext("s1");

    static AgentTool tool(String name, BiFunction<String, ToolContext, String> action) {
        return new AgentTool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(name, "descrição", "{\"type\":\"object\"}");
            }

            @Override
            public String execute(String argumentsJson, ToolContext context) {
                return action.apply(argumentsJson, context);
            }
        };
    }

    @Test
    void executesTheRequestedToolWithSessionContext() {
        var registry = new ToolRegistry(List.of(tool("echo", (args, ctx) -> args + "@" + ctx.sessionId())));

        assertEquals("{\"a\":1}@s1", registry.execute(new ToolCall("1", "echo", "{\"a\":1}"), CONTEXT));
    }

    @Test
    void unknownToolReturnsErrorListingAvailableTools() {
        var registry = new ToolRegistry(List.of(tool("echo", (a, c) -> "ok")));

        String result = registry.execute(new ToolCall("1", "drop_database", "{}"), CONTEXT);

        assertTrue(result.startsWith("{\"error\":"));
        assertTrue(result.contains("echo"));
    }

    @Test
    void toolExceptionMessageGoesBackToTheModel() {
        var registry = new ToolRegistry(List.of(tool("echo", (a, c) -> {
            throw new ToolException("query é obrigatória");
        })));

        assertEquals("{\"error\":\"query é obrigatória\"}", registry.execute(new ToolCall("1", "echo", "{}"), CONTEXT));
    }

    @Test
    void unexpectedFailureDoesNotLeakInternalDetails() {
        var registry = new ToolRegistry(List.of(tool("echo", (a, c) -> {
            throw new IllegalStateException("senha do banco: 123");
        })));

        String result = registry.execute(new ToolCall("1", "echo", "{}"), CONTEXT);

        assertFalse(result.contains("senha"));
        assertTrue(result.contains("Falha interna"));
    }

    @Test
    void rejectsOversizedArguments() {
        var registry = new ToolRegistry(List.of(tool("echo", (a, c) -> "ok")));
        String huge = "x".repeat(ToolRegistry.MAX_ARGUMENTS_LENGTH + 1);

        assertTrue(registry.execute(new ToolCall("1", "echo", huge), CONTEXT).contains("grandes demais"));
    }

    @Test
    void errorJsonEscapesSpecialCharacters() {
        assertEquals("{\"error\":\"a\\\"b\\\\c\\nd\"}", ToolRegistry.errorJson("a\"b\\c\nd"));
    }

    @Test
    void rejectsDuplicateToolNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new ToolRegistry(List.of(tool("echo", (a, c) -> "1"), tool("echo", (a, c) -> "2"))));
    }

    @Test
    void listsDefinitions() {
        var registry = new ToolRegistry(List.of(tool("a", (x, c) -> "1"), tool("b", (x, c) -> "2")));

        assertEquals(List.of("a", "b"), registry.definitions().stream().map(ToolDefinition::name).toList());
    }
}