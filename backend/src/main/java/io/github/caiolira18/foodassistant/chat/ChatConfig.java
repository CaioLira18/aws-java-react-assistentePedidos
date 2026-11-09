package io.github.caiolira18.foodassistant.chat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.caiolira18.foodassistant.assistant.agent.AgentOrchestrator;
import io.github.caiolira18.foodassistant.assistant.agent.AgentSettings;
import io.github.caiolira18.foodassistant.assistant.agent.ToolRegistry;
import io.github.caiolira18.foodassistant.assistant.llm.LlmClient;

@Configuration
class ChatConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatConfig.class);

    @Bean
    AgentOrchestrator agentOrchestrator(ObjectProvider<LlmClient> llm, ToolRegistry tools) {
        LlmClient client = llm.getIfAvailable();
        if (client == null) {
            log.warn("Nenhum LlmClient configurado: o chat responderá com erro até existir o adaptador do Bedrock");
            client = new UnavailableLlmClient();
        }
        return new AgentOrchestrator(client, tools, AgentSettings.defaults());
    }

    @Bean
    ExecutorService chatExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}