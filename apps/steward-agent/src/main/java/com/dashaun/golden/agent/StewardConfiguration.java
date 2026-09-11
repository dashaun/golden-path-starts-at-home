package com.dashaun.golden.agent;

import java.util.List;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class StewardConfiguration {

    /**
     * A hard ceiling on tool calls for one question.
     *
     * Asking for something that is not in the repository gives the model no
     * natural place to stop, and it will keep searching until something times
     * out. The budget in the system prompt is a request; this is the limit.
     * When it is reached the model is told so and has to answer with what it
     * already has.
     */
    @Bean
    ToolCallingManager toolCallingManager(ObservationRegistry observationRegistry,
                                          ToolCallbackResolver toolCallbackResolver,
                                          ToolExecutionExceptionProcessor toolExecutionExceptionProcessor,
                                          StewardProperties properties) {
        return DefaultToolCallingManager.builder()
                .observationRegistry(observationRegistry)
                .toolCallbackResolver(toolCallbackResolver)
                .toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
                .maxTotalToolCalls(properties.maxToolCalls())
                .onLimitExceeded(ToolCallLimitBehavior.RETURN_ERROR_RESPONSE)
                .build();
    }

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
                        return.

                        You have %d tool calls for a single question and no more. Spend
                        them deliberately. If what was asked for is not in the repository,
                        say so plainly and stop; searching harder will not find it.
                        """.formatted(properties.name(), properties.maxToolCalls()))
                .defaultToolCallbacks(toolCallbackProviders.toArray(new ToolCallbackProvider[0]))
                .build();
    }
}
