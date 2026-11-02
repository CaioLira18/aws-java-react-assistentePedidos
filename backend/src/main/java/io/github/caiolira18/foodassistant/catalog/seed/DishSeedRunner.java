package io.github.caiolira18.foodassistant.catalog.seed;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
class DishSeedRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DishSeedRunner.class);

    private static final String INSERT = """
            INSERT INTO dish (name, description, price_cents, prep_minutes, dietary_tags)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    DishSeedRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        Integer existing = jdbc.queryForObject("SELECT count(*) FROM dish", Integer.class);
        if (existing != null && existing > 0) {
            log.info("Seed ignorado: a tabela dish já tem {} pratos", existing);
            return;
        }

        List<DishSeed> dishes = DishSeedGenerator.generate();
        jdbc.batchUpdate(INSERT, dishes, 50, (ps, dish) -> {
            ps.setString(1, dish.name());
            ps.setString(2, dish.description());
            ps.setInt(3, dish.priceCents());
            ps.setInt(4, dish.prepMinutes());
            ps.setArray(5, ps.getConnection().createArrayOf("text", dish.dietaryTags().toArray(new String[0])));
        });
        log.info("Seed concluído: {} pratos inseridos", dishes.size());
    }
}
