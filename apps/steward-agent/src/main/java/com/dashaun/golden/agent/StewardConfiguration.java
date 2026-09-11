package com.dashaun.golden.agent;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StewardConfiguration {

    /**
     * The builder already carries the AGENTS.md advisor, which the starter
     * attaches for us. All that is left is to hand the model the tools the
     * MCP server is willing to expose.
     */
    @Bean
    ChatClient stewardChatClient(ChatClient.Builder builder,
                                 List<ToolCallbackProvider> toolCallbackProviders,
                                 StewardProperties properties) {
        return builder
                .defaultSystem("""
                        You are %s. You answer questions about one repository.

                        You cannot see the repository directly. Use the provided tools to
                        list, read, and search it, and answer only from what those tools
                        return. Say plainly when something is not in the repository.

                        Stay within %d tool calls for a single question.
                        """.formatted(properties.name(), properties.maxToolCalls()))
                .defaultToolCallbacks(toolCallbackProviders.toArray(new ToolCallbackProvider[0]))
                .build();
    }
}
