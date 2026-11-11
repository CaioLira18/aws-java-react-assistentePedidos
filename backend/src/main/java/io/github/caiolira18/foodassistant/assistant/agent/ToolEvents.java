package io.github.caiolira18.foodassistant.assistant.agent;

import java.util.List;

/** Canal para a tool avisar a quem chamou o turno (por exemplo, o chat) sobre algo encontrado. */
public interface ToolEvents {

    ToolEvents NONE = dishes -> { };

    void dishesFound(List<DishSummary> dishes);
}