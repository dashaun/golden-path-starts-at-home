package com.dashaun.golden.mcp;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The whole security posture of this server is these four values, and they
 * arrive from the config server rather than from a shell that started the app.
 *
 * @param workspaceRoot     the only directory this server will ever read from
 * @param maxFileBytes      largest single file an agent may pull back
 * @param allowedExtensions file suffixes an agent may read, without the dot
 * @param maxResults        cap on entries returned by a listing or a search
 */
@ConfigurationProperties(prefix = "golden.mcp")
public record WorkspaceProperties(String workspaceRoot,
                                  long maxFileBytes,
                                  List<String> allowedExtensions,
                                  int maxResults) {

    public WorkspaceProperties {
        workspaceRoot = (workspaceRoot == null || workspaceRoot.isBlank()) ? "." : workspaceRoot;
        maxFileBytes = maxFileBytes <= 0 ? 262_144 : maxFileBytes;
        allowedExtensions = (allowedExtensions == null || allowedExtensions.isEmpty())
                ? List.of("java", "xml", "yml", "yaml", "md", "properties")
                : allowedExtensions;
        maxResults = maxResults <= 0 ? 200 : maxResults;
    }
}
