package com.dashaun.golden.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(WorkspaceProperties.class)
public class CodeMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeMcpServerApplication.class, args);
    }

    @Bean
    ToolCallbackProvider repositoryToolCallbacks(RepositoryTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
