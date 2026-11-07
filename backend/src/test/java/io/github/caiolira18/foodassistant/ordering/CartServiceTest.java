package io.github.caiolira18.foodassistant.ordering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import io.github.caiolira18.foodassistant.TestPostgres;

class CartServiceTest {

    private static JdbcTemplate jdbc;
    private static CartService service;
    private static long feijoada;
    private static long salada;
    private static long inativo;

    @BeforeAll
    static void setUp() {
        String adminUrl = TestPostgres.jdbcUrl();
        new JdbcTemplate(new DriverManagerDataSource(adminUrl, "test", "test")).execute("CREATE DATABASE cart_test");

        var dataSource = new DriverManagerDataSource(
                adminUrl.substring(0, adminUrl.lastIndexOf('/')) + "/cart_test", "test", "test");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        jdbc = new JdbcTemplate(dataSource);
        service = new CartService(new CartRepository(jdbc));

        feijoada = insertDish("Feijoada Vegana", 3190, true);
        salada = insertDish("Salada Caesar", 2690, true);
        inativo = insertDish("Prato Inativo", 1000, false);
        for (int i = 1; i <= 20; i++) {
            insertDish(String.format("Extra %02d", i), 1000, true);
        }
    }

    private static long insertDish(String name, int price, boolean active) {
        return jdbc.queryForObject("""
                INSERT INTO dish (name, description, price_cents, prep_minutes, active)
                VALUES (?, 'desc', ?, 10, ?) RETURNING id
                """, Long.class, name, price, active);
    }

    private static String session() {
        return UUID.randomUUID().toString();
    }

    @Test
    void addsAnItemUsingThePriceFromTheDatabase() {
        CartView cart = service.addItem(session(), feijoada, 2);

        assertEquals(1, cart.items().size());
        CartLine line = cart.items().get(0);
        assertEquals("Feijoada Vegana", line.name());
        assertEquals(3190, line.unitPriceCents());
        assertEquals(6380, line.lineTotalCents());
        assertEquals(2, cart.totalItems());
        assertEquals(6380, cart.totalCents());
    }

    @Test
    void addingTheSameDishTwiceSumsTheQuantity() {
        String session = session();
        service.addItem(session, feijoada, 2);
        CartView cart = service.addItem(session, feijoada, 3);

        assertEquals(5, cart.items().get(0).quantity());
    }

    @Test
    void rejectsMoreThanTheMaximumQuantityPerItem() {
        String session = session();
        service.addItem(session, feijoada, 8);

        assertThrows(IllegalArgumentException.class, () -> service.addItem(session, feijoada, 3));
        assertEquals(8, service.view(session).items().get(0).quantity());
    }

    @Test
    void rejectsInactiveOrMissingDishes() {
        assertThrows(IllegalArgumentException.class, () -> service.addItem(session(), inativo, 1));
        assertThrows(IllegalArgumentException.class, () -> service.addItem(session(), 999_999L, 1));
    }

    @Test
    void rejectsInvalidInput() {
        String session = session();
        assertThrows(IllegalArgumentException.class, () -> service.addItem(session, feijoada, 0));
        assertThrows(IllegalArgumentException.class, () -> service.addItem(session, feijoada, 11));
        assertThrows(IllegalArgumentException.class, () -> service.addItem(session, feijoada, null));
        assertThrows(IllegalArgumentException.class, () -> service.addItem(session, null, 1));
        assertThrows(IllegalArgumentException.class, () -> service.addItem("curto", feijoada, 1));
        assertThrows(IllegalArgumentException.class, () -> service.addItem("sessão com espaços e acento", feijoada, 1));
        assertThrows(IllegalArgumentException.class, () -> service.view(null));
    }

    @Test
    void setsRemovesAndClears() {
        String session = session();
        service.addItem(session, feijoada, 2);
        service.addItem(session, salada, 1);

        assertEquals(7, service.setQuantity(session, feijoada, 6).totalItems());
        assertEquals(1, service.removeItem(session, feijoada).items().size());
        assertTrue(service.clear(session).items().isEmpty());
    }

    @Test
    void viewOfAnUnknownSessionIsEmptyAndDoesNotCreateACart() {
        String session = session();

        assertTrue(service.view(session).items().isEmpty());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM cart WHERE session_id = ?", Integer.class, session));
    }

    @Test
    void cartsAreIsolatedBySession() {
        String a = session();
        String b = session();
        service.addItem(a, feijoada, 1);

        assertTrue(service.view(b).items().isEmpty());
    }

    @Test
    void rejectsMoreThanTheMaximumDistinctItems() {
        String session = session();
        for (int i = 1; i <= 20; i++) {
            long id = jdbc.queryForObject("SELECT id FROM dish WHERE name = ?", Long.class, String.format("Extra %02d", i));
            service.addItem(session, id, 1);
        }

        assertThrows(IllegalArgumentException.class, () -> service.addItem(session, feijoada, 1));
        assertEquals(20, service.view(session).items().size());
    }
}