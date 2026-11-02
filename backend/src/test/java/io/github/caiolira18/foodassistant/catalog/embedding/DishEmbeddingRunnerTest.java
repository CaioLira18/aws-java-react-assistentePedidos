package io.github.caiolira18.foodassistant.catalog.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
   

class DishEmbeddingRunnerTest {

    @Test
    void textIncludesDietaryTagsInNaturalLanguage() {
        String text = DishEmbeddingRunner.textFor("Moqueca", "Palmito em leite de coco.", List.of("vegano", "sem-gluten"));
        assertEquals("Moqueca. Palmito em leite de coco. Restrições: vegano, sem gluten.", text);
    }

    @Test
    void textWithoutTagsHasNoRestrictionSuffix() {
        assertEquals("Feijoada. Completa.", DishEmbeddingRunner.textFor("Feijoada", "Completa.", List.of()));
    }

    @Test
    void vectorLiteralUsesPgvectorFormat() {
        assertEquals("[0.5,-1.0]", VectorLiteral.of(new float[] {0.5f, -1.0f}));
    }

    @Test
    void fakeClientIsDeterministic() {
        FakeEmbeddingClient fake = new FakeEmbeddingClient(1024);
        assertArrayEquals(fake.embed("vegano"), fake.embed("vegano"));
    }
}
