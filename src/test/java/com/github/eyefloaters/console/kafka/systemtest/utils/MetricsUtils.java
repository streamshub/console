package com.github.eyefloaters.console.kafka.systemtest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MetricsUtils {

    private final URL metricsUrl;

    public MetricsUtils(URL metricsUrl) {
        this.metricsUrl = metricsUrl;
    }

    public List<String> getMetrics() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(metricsUrl.openStream()))) {
            return in.lines().collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public BigDecimal getMetricDiff(List<String> preMetrics, List<String> postMetrics, String nameRegex) {
        Pattern namePattern = Pattern.compile(nameRegex);
        BigDecimal preValue = getMetricValue(preMetrics, namePattern);
        BigDecimal postValue = getMetricValue(postMetrics, namePattern);
        return postValue.subtract(preValue);
    }

    public BigDecimal getMetricValue(List<String> metrics, Pattern namePattern) {
        return metrics.stream()
            .filter(record -> !record.startsWith("#"))
            .filter(record -> namePattern.matcher(record).find())
            .map(record -> record.split(" +"))
            .map(fields -> {
                return new BigDecimal(fields[1]);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
