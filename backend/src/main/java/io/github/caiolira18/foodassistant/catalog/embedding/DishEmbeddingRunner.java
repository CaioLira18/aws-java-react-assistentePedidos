package io.github.caiolira18.foodassistant.catalog.embedding;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("embed")
class DishEmbeddingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DishEmbeddingRunner.class);

    private record DishRow(long id, String name, String description, List<String> tags) {
    }

    private final JdbcTemplate jdbc;
    private final EmbeddingClient embeddings;

    DishEmbeddingRunner(JdbcTemplate jdbc, EmbeddingClient embeddings) {
        this.jdbc = jdbc;
        this.embeddings = embeddings;
    }

    @Override
    public void run(String... args) {
        List<DishRow> dishes = jdbc.query(
                "SELECT id, name, description, dietary_tags FROM dish WHERE embedding IS NULL ORDER BY id",
                (rs, i) -> new DishRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        Arrays.asList((String[]) rs.getArray("dietary_tags").getArray())));

        log.info("{} pratos sem embedding", dishes.size());
        int done = 0;
        for (DishRow dish : dishes) {
            float[] vector = embeddings.embed(textFor(dish.name(), dish.description(), dish.tags()));
            jdbc.update("UPDATE dish SET embedding = ?::vector WHERE id = ?", VectorLiteral.of(vector), dish.id());
            if (++done % 25 == 0) {
                log.info("{}/{} embeddings gerados", done, dishes.size());
            }
        }
        log.info("Concluído: {} embeddings gerados", done);
    }

    static String textFor(String name, String description, List<String> tags) {
        String text = name + ". " + description;
        if (!tags.isEmpty()) {
            text += " Restrições: " + String.join(", ", tags).replace('-', ' ') + ".";
        }
        return text;
    }
}
