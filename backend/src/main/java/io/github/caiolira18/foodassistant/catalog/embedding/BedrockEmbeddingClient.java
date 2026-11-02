package io.github.caiolira18.foodassistant.catalog.embedding;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
class BedrockEmbeddingClient implements EmbeddingClient {

    private final BedrockRuntimeClient client;
    private final String modelId;
    private final int dimensions;
    private final JsonMapper mapper = JsonMapper.builder().build();

    BedrockEmbeddingClient(
            BedrockRuntimeClient client,
            @Value("${bedrock.embedding.model-id}") String modelId,
            @Value("${bedrock.embedding.dimensions}") int dimensions) {
        this.client = client;
        this.modelId = modelId;
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        String body = mapper.writeValueAsString(Map.of(
                "inputText", text,
                "dimensions", dimensions,
                "normalize", true));

        InvokeModelResponse response = client.invokeModel(r -> r
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(body)));

        JsonNode embedding = mapper.readTree(response.body().asUtf8String()).get("embedding");
        if (embedding == null || embedding.size() != dimensions) {
            throw new IllegalStateException("Resposta do Bedrock com dimensão inesperada");
        }
        float[] vector = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            vector[i] = embedding.get(i).floatValue();
        }
        return vector;
    }
}
