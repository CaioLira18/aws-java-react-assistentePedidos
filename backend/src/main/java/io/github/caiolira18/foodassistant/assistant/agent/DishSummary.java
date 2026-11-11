package io.github.caiolira18.foodassistant.assistant.agent;

import java.util.List;

public record DishSummary(long id, String name, String description, int priceCents, int prepMinutes, List<String> dietaryTags) {
  public DishSummary {
    dietaryTags = List.copyOf(dietaryTags);
  }
}