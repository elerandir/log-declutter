package com.elerandir.logdeclutter;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Converts a single structured (JSON object) log line into a classic Logback/Spring-style line:
 * {@code <timestamp>  <LEVEL> [<thread>] <logger> - <message>}, filling {@code [thread]} and
 * {@code logger} only when present.
 *
 * <p>Nested JSON objects are flattened to dotted keys, so both {@code {"log.level":"INFO"}} and
 * {@code {"log":{"level":"INFO"}}} resolve. Field names are looked up against a list of common
 * candidates (ECS and typical logging-framework conventions).
 */
@Singleton
public class JsonLogConverter {

    private static final List<String> TIMESTAMP_KEYS =
            List.of("@timestamp", "timestamp", "time", "ts");
    private static final List<String> LEVEL_KEYS =
            List.of("log.level", "level", "severity", "loglevel");
    private static final List<String> MESSAGE_KEYS = List.of("message", "msg");
    private static final List<String> LOGGER_KEYS =
            List.of("log.logger", "logger.name", "logger", "loggerName", "logger_name");
    private static final List<String> THREAD_KEYS =
            List.of("process.thread.name", "thread.name", "thread", "threadName", "thread_name");

    private final ObjectMapper objectMapper;

    @Inject
    public JsonLogConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Converts the line if it is a JSON object.
     *
     * @param line the line to convert
     * @return the classic-style line, or {@link Optional#empty()} if the line is not a JSON object
     *     (in which case the caller should pass the original line through unchanged)
     */
    public Optional<String> convert(String line) {
        String trimmed = line.strip();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return Optional.empty();
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(trimmed);
        } catch (JacksonException e) {
            return Optional.empty();
        }
        if (node == null || !node.isObject()) {
            return Optional.empty();
        }

        Map<String, String> fields = new LinkedHashMap<>();
        flatten("", node, fields);
        return Optional.of(format(fields));
    }

    /** Recursively flattens a JSON node into dotted-path string values. */
    private void flatten(String prefix, JsonNode node, Map<String, String> out) {
        if (node.isObject()) {
            node.properties()
                    .forEach(
                            entry ->
                                    flatten(
                                            prefix.isEmpty()
                                                    ? entry.getKey()
                                                    : prefix + "." + entry.getKey(),
                                            entry.getValue(),
                                            out));
        } else if (node.isNull()) {
            // Ignore explicit nulls so absent-vs-null fields behave the same.
            return;
        } else if (node.isValueNode()) {
            out.put(prefix, node.asText());
        } else {
            // Arrays and other containers: keep their JSON text at this path.
            out.put(prefix, node.toString());
        }
    }

    /** Builds the classic-style line from the flattened fields. */
    private String format(Map<String, String> fields) {
        String timestamp = firstPresent(fields, TIMESTAMP_KEYS);
        String level = firstPresent(fields, LEVEL_KEYS);
        String thread = firstPresent(fields, THREAD_KEYS);
        String logger = firstPresent(fields, LOGGER_KEYS);
        String message = firstPresent(fields, MESSAGE_KEYS);

        List<String> header = new ArrayList<>();
        if (!timestamp.isEmpty()) {
            header.add(timestamp);
        }
        if (!level.isEmpty()) {
            header.add(String.format("%" + LogDeclutterConstants.CLASSIC_LEVEL_WIDTH + "s", level));
        }
        if (!thread.isEmpty()) {
            header.add("[" + thread + "]");
        }
        if (!logger.isEmpty()) {
            header.add(logger);
        }
        return String.join(" ", header) + LogDeclutterConstants.CLASSIC_MESSAGE_SEPARATOR + message;
    }

    /** Returns the value of the first candidate key present, or an empty string if none match. */
    private String firstPresent(Map<String, String> fields, List<String> candidateKeys) {
        for (String key : candidateKeys) {
            String value = fields.get(key);
            if (value != null) {
                return value;
            }
        }
        return "";
    }
}
