package io.github.caiolira18.foodassistant.catalog.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import io.github.caiolira18.foodassistant.catalog.embedding.VectorLiteral;

@Repository
class DishSearchRepository {

    private final JdbcTemplate jdbc;

    DishSearchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<DishSearchResult> search(float[] queryEmbedding, DishSearchFilters filters, int limit) {
        String vector = VectorLiteral.of(queryEmbedding);
        List<Object> args = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT id, name, description, price_cents, prep_minutes, dietary_tags,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM dish
                WHERE active AND embedding IS NOT NULL
                """);
        args.add(vector);

        if (filters.maxPriceCents() != null) {
            sql.append(" AND price_cents <= ?\n");
            args.add(filters.maxPriceCents());
        }
        if (filters.maxPrepMinutes() != null) {
            sql.append(" AND prep_minutes <= ?\n");
            args.add(filters.maxPrepMinutes());
        }
        if (!filters.requiredTags().isEmpty()) {
            sql.append(" AND dietary_tags @> ?\n");
            args.add(filters.requiredTags().toArray(String[]::new));
        }

        sql.append(" ORDER BY embedding <=> ?::vector\n LIMIT ?");
        args.add(vector);
        args.add(limit);

        return jdbc.query(sql.toString(), ps -> {
            for (int i = 0; i < args.size(); i++) {
                Object arg = args.get(i);
                if (arg instanceof String[] tags) {
                    ps.setArray(i + 1, ps.getConnection().createArrayOf("text", tags));
                } else {
                    ps.setObject(i + 1, arg);
                }
            }
        }, (rs, rowNum) -> new DishSearchResult(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("price_cents"),
                rs.getInt("prep_minutes"),
                Arrays.asList((String[]) rs.getArray("dietary_tags").getArray()),
                rs.getDouble("similarity")));
    }
}
