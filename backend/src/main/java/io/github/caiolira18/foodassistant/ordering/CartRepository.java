package io.github.caiolira18.foodassistant.ordering;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CartRepository {

    private final JdbcTemplate jdbc;

    CartRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    UUID lockOrCreateCart(String sessionId) {
        jdbc.update("INSERT INTO cart (id, session_id) VALUES (?, ?) ON CONFLICT (session_id) DO NOTHING",
                UUID.randomUUID(), sessionId);
        return findCart(sessionId, true).orElseThrow();
    }

    Optional<UUID> findCart(String sessionId, boolean lock) {
        String sql = "SELECT id FROM cart WHERE session_id = ?" + (lock ? " FOR UPDATE" : "");
        return jdbc.query(sql, (rs, i) -> rs.getObject(1, UUID.class), sessionId).stream().findFirst();
    }

    boolean dishIsActive(long dishId) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM dish WHERE id = ? AND active", Integer.class, dishId);
        return count != null && count > 0;
    }

    int quantityOf(UUID cartId, long dishId) {
        return jdbc.query("SELECT quantity FROM cart_item WHERE cart_id = ? AND dish_id = ?",
                (rs, i) -> rs.getInt(1), cartId, dishId).stream().findFirst().orElse(0);
    }

    int distinctItems(UUID cartId) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM cart_item WHERE cart_id = ?", Integer.class, cartId);
        return count == null ? 0 : count;
    }

    void upsertItem(UUID cartId, long dishId, int quantity) {
        jdbc.update("""
                INSERT INTO cart_item (cart_id, dish_id, quantity) VALUES (?, ?, ?)
                ON CONFLICT (cart_id, dish_id) DO UPDATE SET quantity = EXCLUDED.quantity
                """, cartId, dishId, quantity);
    }

    void removeItem(UUID cartId, long dishId) {
        jdbc.update("DELETE FROM cart_item WHERE cart_id = ? AND dish_id = ?", cartId, dishId);
    }

    void clear(UUID cartId) {
        jdbc.update("DELETE FROM cart_item WHERE cart_id = ?", cartId);
    }

    List<CartLine> lines(UUID cartId) {
        return jdbc.query("""
                SELECT d.id, d.name, d.price_cents, ci.quantity
                FROM cart_item ci JOIN dish d ON d.id = ci.dish_id
                WHERE ci.cart_id = ?
                ORDER BY d.name
                """, (rs, i) -> {
            int price = rs.getInt("price_cents");
            int quantity = rs.getInt("quantity");
            return new CartLine(rs.getLong("id"), rs.getString("name"), price, quantity, price * quantity);
        }, cartId);
    }
}