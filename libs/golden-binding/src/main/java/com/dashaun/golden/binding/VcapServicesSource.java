package com.dashaun.golden.binding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * The Cloud Foundry layout: one JSON document in VCAP_SERVICES holding every
 * bound instance. A binding's type comes from its tags, so a service created
 * with {@code cf cups ... -t config} looks the same to an application as a
 * directory named "config" on a laptop.
 */
public class VcapServicesSource implements BindingSource {

    static final String VCAP_VARIABLE = "VCAP_SERVICES";

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @Override
    @SuppressWarnings("unchecked")
    public List<ServiceBinding> bindings(Environment environment) {
        String vcap = environment.getProperty(VCAP_VARIABLE);
        if (!StringUtils.hasText(vcap)) {
            return List.of();
        }
        Map<String, Object> parsed;
        try {
            parsed = jsonParser.parseMap(vcap);
        }
        catch (RuntimeException ex) {
            return List.of();
        }

        List<ServiceBinding> found = new ArrayList<>();
        for (Map.Entry<String, Object> label : parsed.entrySet()) {
            if (!(label.getValue() instanceof List<?> instances)) {
                continue;
            }
            for (Object element : instances) {
                if (element instanceof Map<?, ?> instance) {
                    add(found, label.getKey(), (Map<String, Object>) instance);
                }
            }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private void add(List<ServiceBinding> found, String label, Map<String, Object> instance) {
        String name = String.valueOf(instance.getOrDefault("name", label));
        Map<String, String> credentials = new LinkedHashMap<>();
        if (instance.get("credentials") instanceof Map<?, ?> raw) {
            ((Map<String, Object>) raw).forEach((key, value) -> {
                if (value != null) {
                    credentials.put(key, String.valueOf(value));
                }
            });
        }
        if (instance.get("tags") instanceof List<?> tags) {
            for (Object tag : tags) {
                found.add(new ServiceBinding(name, String.valueOf(tag), VCAP_VARIABLE, credentials));
            }
        }
    }
}
