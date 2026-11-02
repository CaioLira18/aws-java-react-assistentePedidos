package io.github.caiolira18.foodassistant.catalog.search;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("search")
class DishSearchRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DishSearchRunner.class);

    private final DishSearchService service;
    private final String query;
    private final String maxPriceCents;
    private final String maxPrepMinutes;
    private final String tags;

    DishSearchRunner(
            DishSearchService service,
            @Value("${search.query}") String query,
            @Value("${search.max-price-cents:}") String maxPriceCents,
            @Value("${search.max-prep-minutes:}") String maxPrepMinutes,
            @Value("${search.tags:}") String tags) {
        this.service = service;
        this.query = query;
        this.maxPriceCents = maxPriceCents;
        this.maxPrepMinutes = maxPrepMinutes;
        this.tags = tags;
    }

    @Override
    public void run(String... args) {
        Set<String> requiredTags = tags.isBlank()
                ? Set.of()
                : Arrays.stream(tags.split(",")).map(String::trim).collect(Collectors.toSet());
        DishSearchFilters filters = new DishSearchFilters(
                maxPriceCents.isBlank() ? null : Integer.valueOf(maxPriceCents),
                maxPrepMinutes.isBlank() ? null : Integer.valueOf(maxPrepMinutes),
                requiredTags);

        List<DishSearchResult> results = service.search(query, filters, 5);
        log.info("Consulta: \"{}\" | filtros: {}", query, filters);
        for (DishSearchResult r : results) {
            log.info("{} | R$ {} | {} min | {} | sim={}",
                    r.name(), String.format("%.2f", r.priceCents() / 100.0), r.prepMinutes(),
                    r.dietaryTags(), String.format("%.3f", r.similarity()));
        }
    }
}
