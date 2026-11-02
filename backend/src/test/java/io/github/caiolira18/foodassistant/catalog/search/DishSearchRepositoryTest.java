package io.github.caiolira18.foodassistant.catalog.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.github.caiolira18.foodassistant.catalog.embedding.VectorLiteral;

class DishSearchRepositoryTest {

    private static final int DIMENSIONS = 1024;

    private static GenericContainer<?> postgres;
    private static JdbcTemplate jdbc;
    private static DishSearchRepository repository;

    @BeforeAll
    static void startDatabase() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker indisponível");

        postgres = new GenericContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
                .withEnv("POSTGRES_DB", "test")
                .withEnv("POSTGRES_USER", "test")
                .withEnv("POSTGRES_PASSWORD", "test")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)
                        .withStartupTimeout(Duration.ofSeconds(90)));
        postgres.start();

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/test",
                "test", "test");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        jdbc = new JdbcTemplate(dataSource);
        repository = new DishSearchRepository(jdbc);

        insert("Feijoada Vegana", 3190, 45, new String[] {"vegano", "vegetariano", "sem-lactose", "sem-gluten"}, vec(1, 0, 0), true);
        insert("Feijoada Completa", 3990, 50, new String[] {}, vec(0.9f, 0.1f, 0), true);
        insert("Tapioca de Queijo", 1890, 10, new String[] {"vegetariano", "sem-gluten"}, vec(0.5f, 0.5f, 0), true);
        insert("Salada Caesar", 2690, 15, new String[] {}, vec(0, 1, 0), true);
        insert("Prato Inativo", 1000, 10, new String[] {}, vec(1, 0, 0), false);
        insert("Prato Sem Embedding", 1000, 10, new String[] {}, null, true);
    }

    @AfterAll
    static void stopDatabase() {
        if (postgres != null) {
            postgres.stop();
        }
    }

    @Test
    void ranksBySimilarityAndIgnoresInactiveOrUnembeddedDishes() {
        List<String> names = names(DishSearchFilters.none(), 10);

        assertEquals(List.of("Feijoada Vegana", "Feijoada Completa", "Tapioca de Queijo", "Salada Caesar"), names);
    }

    @Test
    void filtersByMaxPrice() {
        assertEquals(List.of("Tapioca de Queijo", "Salada Caesar"),
                names(new DishSearchFilters(3000, null, null), 10));
    }

    @Test
    void filtersByMaxPrepTime() {
        assertEquals(List.of("Tapioca de Queijo", "Salada Caesar"),
                names(new DishSearchFilters(null, 20, null), 10));
    }

    @Test
    void filtersByRequiredTags() {
        assertEquals(List.of("Feijoada Vegana"), names(new DishSearchFilters(null, null, Set.of("vegano")), 10));
        assertEquals(List.of("Feijoada Vegana", "Tapioca de Queijo"),
                names(new DishSearchFilters(null, null, Set.of("vegetariano", "sem-gluten")), 10));
    }

    @Test
    void combinesAllFilters() {
        assertEquals(List.of("Tapioca de Queijo"),
                names(new DishSearchFilters(2000, 15, Set.of("sem-gluten")), 10));
    }

    @Test
    void respectsLimit() {
        assertEquals(List.of("Feijoada Vegana", "Feijoada Completa"), names(DishSearchFilters.none(), 2));
    }

    @Test
    void returnsSimilarityBetweenZeroAndOne() {
        DishSearchResult first = repository.search(vec(1, 0, 0), DishSearchFilters.none(), 1).get(0);
        assertEquals(1.0, first.similarity(), 0.001);
        assertFalse(first.dietaryTags().isEmpty());
    }

    private static List<String> names(DishSearchFilters filters, int limit) {
        return repository.search(vec(1, 0, 0), filters, limit).stream().map(DishSearchResult::name).toList();
    }

    private static float[] vec(float... head) {
        return Arrays.copyOf(head, DIMENSIONS);
    }

    private static void insert(String name, int price, int prep, String[] tags, float[] embedding, boolean active) {
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dish (name, description, price_cents, prep_minutes, dietary_tags, embedding, active)
                    VALUES (?, ?, ?, ?, ?, ?::vector, ?)
                    """);
            ps.setString(1, name);
            ps.setString(2, "Descrição de " + name);
            ps.setInt(3, price);
            ps.setInt(4, prep);
            ps.setArray(5, con.createArrayOf("text", tags));
            ps.setString(6, embedding == null ? null : VectorLiteral.of(embedding));
            ps.setBoolean(7, active);
            return ps;
        });
    }
}
