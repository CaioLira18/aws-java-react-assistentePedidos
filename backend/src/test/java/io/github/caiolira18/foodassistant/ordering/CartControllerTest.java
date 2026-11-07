package io.github.caiolira18.foodassistant.ordering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CartControllerTest {

    private static final String SESSION = "sessao-12345678";

    private final CartService service = mock(CartService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CartController(service)).build();

    private static CartView sample() {
        return new CartView(List.of(new CartLine(1, "Feijoada Vegana", 3190, 2, 6380)), 2, 6380);
    }

    @Test
    void viewReturnsTheCart() throws Exception {
        when(service.view(SESSION)).thenReturn(sample());

        mvc.perform(get("/api/cart").header("X-Session-Id", SESSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCents").value(6380))
                .andExpect(jsonPath("$.items[0].name").value("Feijoada Vegana"));
    }

    @Test
    void addDelegatesToTheService() throws Exception {
        when(service.addItem(SESSION, 1L, 2)).thenReturn(sample());

        mvc.perform(post("/api/cart/items").header("X-Session-Id", SESSION)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dishId\":1,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void validationErrorsBecomeBadRequestWithAMessage() throws Exception {
        when(service.addItem(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("quantity deve estar entre 1 e 10"));

        mvc.perform(post("/api/cart/items").header("X-Session-Id", SESSION)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dishId\":1,\"quantity\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("quantity deve estar entre 1 e 10"));
    }

    @Test
    void missingSessionHeaderIsRejected() throws Exception {
        mvc.perform(delete("/api/cart")).andExpect(status().isBadRequest());
    }
}