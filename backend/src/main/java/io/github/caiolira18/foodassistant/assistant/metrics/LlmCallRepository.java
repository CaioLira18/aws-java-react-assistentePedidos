package io.github.caiolira18.foodassistant.assistant.metrics;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LlmCallRepository {

    private final JdbcTemplate jdbc;

    public LlmCallRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(LlmCallRecord r) {
        jdbc.update("""
                INSERT INTO llm_call (session_id, model, input_tokens, output_tokens, latency_ms, cost_usd)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                r.sessionId(), r.model(), r.inputTokens(), r.outputTokens(), r.latencyMs(), r.costUsd());
    }

    public List<LlmCallRecord> findBySession(String sessionId) {
        return jdbc.query("""
                SELECT session_id, model, input_tokens, output_tokens, latency_ms, cost_usd
                FROM llm_call WHERE session_id = ? ORDER BY id
                """,
                (rs, i) -> new LlmCallRecord(
                        rs.getString("session_id"),
                        rs.getString("model"),
                        (Integer) rs.getObject("input_tokens"),
                        (Integer) rs.getObject("output_tokens"),
                        rs.getInt("latency_ms"),
                        rs.getBigDecimal("cost_usd")),
                sessionId);
    }
}