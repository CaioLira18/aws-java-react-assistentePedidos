package io.github.caiolira18.foodassistant;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.github.caiolira18.foodassistant.assistant.agent.ToolRegistry;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;

@SpringBootTest
@Testcontainers
class FoodassistantApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"));

    @Test
    void contextLoads() {
    }

        @Autowired
    private ToolRegistry toolRegistry;

    @Test
    void registersTheCatalogTools() {
        Set<String> names = toolRegistry.definitions().stream()
                .map(ToolDefinition::name).collect(Collectors.toSet());

        assertEquals(Set.of("search_dishes", "get_dish"), names);
    }
}
