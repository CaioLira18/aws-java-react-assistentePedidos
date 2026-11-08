package io.github.caiolira18.foodassistant.ordering;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.caiolira18.foodassistant.shared.SessionIds;

@Service
public class CartService {

    public static final int MAX_QUANTITY_PER_ITEM = 10;
    public static final int MAX_DISTINCT_ITEMS = 20;

    private static final Pattern SESSION_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    private final CartRepository repository;

    CartService(CartRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CartView view(String sessionId) {
        return repository.findCart(validSession(sessionId), false).map(this::build).orElse(CartView.empty());
    }

    @Transactional
    public CartView addItem(String sessionId, Long dishId, Integer quantity) {
        String session = validSession(sessionId);
        long dish = validDish(dishId);
        int qty = validQuantity(quantity);
        UUID cartId = prepare(session, dish);
        int current = repository.quantityOf(cartId, dish);
        checkDistinctLimit(cartId, current);
        if (current + qty > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException(
                    "Máximo de " + MAX_QUANTITY_PER_ITEM + " unidades por item (já há " + current + " no carrinho)");
        }
        repository.upsertItem(cartId, dish, current + qty);
        return build(cartId);
    }

    @Transactional
    public CartView setQuantity(String sessionId, Long dishId, Integer quantity) {
        String session = validSession(sessionId);
        long dish = validDish(dishId);
        int qty = validQuantity(quantity);
        UUID cartId = prepare(session, dish);
        checkDistinctLimit(cartId, repository.quantityOf(cartId, dish));
        repository.upsertItem(cartId, dish, qty);
        return build(cartId);
    }

    @Transactional
    public CartView removeItem(String sessionId, Long dishId) {
        String session = validSession(sessionId);
        long dish = validDish(dishId);
        return repository.findCart(session, true).map(cartId -> {
            repository.removeItem(cartId, dish);
            return build(cartId);
        }).orElse(CartView.empty());
    }

    @Transactional
    public CartView clear(String sessionId) {
        return repository.findCart(validSession(sessionId), true).map(cartId -> {
            repository.clear(cartId);
            return build(cartId);
        }).orElse(CartView.empty());
    }

    private UUID prepare(String session, long dish) {
        if (!repository.dishIsActive(dish)) {
            throw new IllegalArgumentException("Prato não encontrado ou indisponível");
        }
        return repository.lockOrCreateCart(session);
    }

    private void checkDistinctLimit(UUID cartId, int currentQuantity) {
        if (currentQuantity == 0 && repository.distinctItems(cartId) >= MAX_DISTINCT_ITEMS) {
            throw new IllegalArgumentException(
                    "O carrinho aceita no máximo " + MAX_DISTINCT_ITEMS + " itens diferentes");
        }
    }

    private CartView build(UUID cartId) {
        List<CartLine> lines = repository.lines(cartId);
        return new CartView(lines,
                lines.stream().mapToInt(CartLine::quantity).sum(),
                lines.stream().mapToInt(CartLine::lineTotalCents).sum());
    }

    private static String validSession(String sessionId) {
        return SessionIds.validate(sessionId);
    }

    private static long validDish(Long dishId) {
        if (dishId == null || dishId < 1) {
            throw new IllegalArgumentException("dishId inválido");
        }
        return dishId;
    }

    private static int validQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException("quantity deve estar entre 1 e " + MAX_QUANTITY_PER_ITEM);
        }
        return quantity;
    }
}