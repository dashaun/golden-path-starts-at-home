package com.dashaun.golden.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryToolsTests {

    @TempDir
    Path workspace;

    private RepositoryTools tools;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(workspace.resolve("README.md"), "the golden path\n");
        Files.createDirectories(workspace.resolve("config-repo"));
        Files.writeString(workspace.resolve("config-repo/application.yml"), "golden:\n  platform: kind\n");
        Files.writeString(workspace.resolve("secrets.bin"), "not text");

        this.tools = new RepositoryTools(new WorkspaceProperties(
                workspace.toString(), 1024, List.of("md", "yml"), 50));
    }

    @Test
    void treatsALeadingSlashAsTheRepositoryRoot() {
        assertThat(tools.listFiles("/")).containsExactlyInAnyOrder(
                "README.md", "config-repo/", "secrets.bin");
        assertThat(tools.listFiles("/config-repo")).containsExactly("config-repo/application.yml");
    }

    @Test
    void listsTheRootWhenGivenNothing() {
        assertThat(tools.listFiles("")).isEqualTo(tools.listFiles("."));
        assertThat(tools.listFiles(null)).isEqualTo(tools.listFiles("."));
    }

    @Test
    void readsAnAllowedFile() {
        assertThat(tools.readFile("config-repo/application.yml")).contains("platform: kind");
    }

    @Test
    void refusesAnExtensionItWasNotGiven() {
        assertThat(tools.readFile("secrets.bin")).startsWith("Refused:");
    }

    @Test
    void refusesAFileLargerThanTheLimit() throws Exception {
        Files.writeString(workspace.resolve("big.md"), "x".repeat(2048));

        assertThat(tools.readFile("big.md")).contains("over the");
    }

    @Test
    void refusesToClimbOutOfTheWorkspace() {
        assertThatThrownBy(() -> tools.readFile("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the workspace");
        assertThatThrownBy(() -> tools.readFile("/../escape.md"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outside the workspace");
    }

    @Test
    void findsTextAndReportsWhereItIs() {
        assertThat(tools.searchRepository("golden"))
                .anySatisfy(hit -> assertThat(hit).startsWith("README.md:1:"));
        assertThat(tools.searchRepository("nothing here")).containsExactly("No matches for: nothing here");
    }
}
