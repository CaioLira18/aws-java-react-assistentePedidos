package io.github.caiolira18.foodassistant.assistant.agent;

/** Erro de uso da tool (argumento inválido etc.). A mensagem volta para o modelo. */
public class ToolException extends RuntimeException {

    public ToolException(String message) {
        super(message);
    }
}