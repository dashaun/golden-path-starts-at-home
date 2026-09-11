package com.dashaun.golden.greeting;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
@EnableConfigurationProperties({GreetingProperties.class, PartnerApiProperties.class})
class GreetingController {

    private final GreetingProperties greeting;
    private final PartnerApiProperties partnerApi;
    private final String configUri;

    GreetingController(GreetingProperties greeting, PartnerApiProperties partnerApi,
                       @Value("${spring.cloud.config.uri:not bound}") String configUri) {
        this.greeting = greeting;
        this.partnerApi = partnerApi;
        this.configUri = configUri;
    }

    @GetMapping("/")
    Map<String, Object> greet() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", greeting.message());
        body.put("audience", greeting.audience());
        body.put("theme", greeting.theme());
        body.put("boundTo", redact(configUri));
        body.put("partnerTenant", partnerApi.tenant());
        body.put("partnerKey", mask(partnerApi.key()));
        return body;
    }

    /**
     * Proves the secret is live without printing it. Rotate the value in Vault,
     * hit /actuator/refresh, and this fingerprint changes.
     */
    @GetMapping("/secret-fingerprint")
    Map<String, Object> fingerprint() {
        String key = partnerApi.key();
        return Map.of(
                "present", key != null && !key.isBlank(),
                "length", key == null ? 0 : key.length(),
                "fingerprint", mask(key));
    }

    private static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "absent";
        }
        int keep = Math.min(4, value.length());
        return value.substring(0, keep) + "…" + value.substring(value.length() - keep);
    }

    private static String redact(String uri) {
        return uri.replaceAll("://[^@/]+@", "://•••@");
    }
}
