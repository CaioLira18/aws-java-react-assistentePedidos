package io.github.caiolira18.foodassistant.catalog.embedding;

public interface EmbeddingClient {

    float[] embed(String text);
}
