package com.dashaun.golden.agent;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StewardController {

    private final ChatClient chatClient;
    private final StewardProperties properties;

    StewardController(ChatClient chatClient, StewardProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    @GetMapping("/")
    Map<String, Object> about() {
        return Map.of(
                "agent", properties.name(),
                "maxToolCalls", properties.maxToolCalls(),
                "ask", "POST /ask with {\"question\": \"...\"}");
    }

    @PostMapping("/ask")
    Map<String, String> ask(@RequestBody Question question) {
        return Map.of("answer", answer(question.question()));
    }

    @GetMapping("/ask")
    Map<String, String> ask(@RequestParam("q") String question) {
        return Map.of("answer", answer(question));
    }

    private String answer(String question) {
        return chatClient.prompt().user(question).call().content();
    }

    record Question(String question) {
    }
}
