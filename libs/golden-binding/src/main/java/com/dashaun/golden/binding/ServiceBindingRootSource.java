package com.dashaun.golden.binding;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * The Kubernetes service binding layout, which is also the easiest thing to
 * hand a laptop: a directory per binding, a file per key, the file name is
 * the key and the file contents are the value.
 *
 * <pre>
 * bindings/golden-config/type      -> config
 * bindings/golden-config/uri       -> https://config-server.example.com
 * bindings/golden-config/username  -> golden
 * bindings/golden-config/password  -> ...
 * </pre>
 */
public class ServiceBindingRootSource implements BindingSource {

    static final String ROOT_VARIABLE = "SERVICE_BINDING_ROOT";

    @Override
    public List<ServiceBinding> bindings(Environment environment) {
        String root = environment.getProperty(ROOT_VARIABLE);
        if (!StringUtils.hasText(root)) {
            return List.of();
        }
        Path rootPath = Paths.get(root);
        if (!Files.isDirectory(rootPath)) {
            return List.of();
        }
        List<ServiceBinding> found = new ArrayList<>();
        try (Stream<Path> directories = Files.list(rootPath)) {
            directories.filter(Files::isDirectory).forEach(directory -> read(directory).ifPresent(found::add));
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return found;
    }

    private java.util.Optional<ServiceBinding> read(Path directory) {
        Map<String, String> values = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile).forEach(file -> values.put(
                    file.getFileName().toString(),
                    readValue(file)));
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        String type = values.remove("type");
        if (!StringUtils.hasText(type)) {
            return java.util.Optional.empty();
        }
        values.remove("provider");
        return java.util.Optional.of(new ServiceBinding(
                directory.getFileName().toString(), type, ROOT_VARIABLE, values));
    }

    private String readValue(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8).trim();
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
