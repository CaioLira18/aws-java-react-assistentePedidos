package io.github.caiolira18.foodassistant.ordering;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
class CartController {

    static final String SESSION_HEADER = "X-Session-Id";

    record AddItemRequest(Long dishId, Integer quantity) {
    }

    record QuantityRequest(Integer quantity) {
    }

    private final CartService cart;

    CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    CartView view(@RequestHeader(SESSION_HEADER) String sessionId) {
        return cart.view(sessionId);
    }

    @PostMapping("/items")
    CartView add(@RequestHeader(SESSION_HEADER) String sessionId, @RequestBody AddItemRequest body) {
        return cart.addItem(sessionId, body.dishId(), body.quantity());
    }

    @PutMapping("/items/{dishId}")
    CartView setQuantity(@RequestHeader(SESSION_HEADER) String sessionId, @PathVariable long dishId,
            @RequestBody QuantityRequest body) {
        return cart.setQuantity(sessionId, dishId, body.quantity());
    }

    @DeleteMapping("/items/{dishId}")
    CartView remove(@RequestHeader(SESSION_HEADER) String sessionId, @PathVariable long dishId) {
        return cart.removeItem(sessionId, dishId);
    }

    @DeleteMapping
    CartView clear(@RequestHeader(SESSION_HEADER) String sessionId) {
        return cart.clear(sessionId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}