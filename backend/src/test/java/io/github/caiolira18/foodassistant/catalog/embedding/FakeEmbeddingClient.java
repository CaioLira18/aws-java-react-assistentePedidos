package io.github.caiolira18.foodassistant.catalog.embedding;

import java.util.Random;

public class FakeEmbeddingClient implements EmbeddingClient {

    private final int dimensions;

    public FakeEmbeddingClient(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        Random random = new Random(text.hashCode());
        float[] vector = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            vector[i] = random.nextFloat() - 0.5f;
        }
        return vector;
    }
}
