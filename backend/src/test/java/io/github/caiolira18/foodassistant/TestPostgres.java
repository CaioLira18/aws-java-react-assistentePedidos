package io.github.caiolira18.foodassistant;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public final class TestPostgres {

    private static GenericContainer<?> container;

    private TestPostgres() {
    }

    public static synchronized String jdbcUrl() {
        if (container == null) {
            container = new GenericContainer<>(DockerImageName.parse("pgvector/pgvector:pg16"))
                    .withEnv("POSTGRES_DB", "test")
                    .withEnv("POSTGRES_USER", "test")
                    .withEnv("POSTGRES_PASSWORD", "test")
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)
                            .withStartupTimeout(Duration.ofSeconds(90)));
            container.start();
        }
        return "jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/test";
    }
}