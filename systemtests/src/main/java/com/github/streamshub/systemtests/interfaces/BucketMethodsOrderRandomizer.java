package com.github.streamshub.systemtests.interfaces;

import com.github.streamshub.systemtests.annotations.TestBucket;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BucketMethodsOrderRandomizer implements MethodOrderer {

    /**
     * Randomizes the execution order of test methods, with support for grouping
     * by {@link TestBucket}.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Methods annotated with {@link TestBucket} are grouped by their bucket name.</li>
     *   <li>Ungrouped methods are collected separately.</li>
     *   <li>Order is randomized in three steps:
     *     <ul>
     *       <li>Shuffle methods within each group.</li>
     *       <li>Shuffle ungrouped methods.</li>
     *       <li>Shuffle the order of groups themselves.</li>
     *     </ul>
     *   </li>
     *   <li>The final flattened list defines the execution order.</li>
     * </ul>
     *
     * <p>This ensures that tests run in a non-deterministic order while keeping
     * all methods in the same {@code @TestBucket} executed together.</p>
     *
     * @param context the JUnit {@link MethodOrdererContext} providing test methods
     */
    @Override
    public void orderMethods(MethodOrdererContext context) {
        @SuppressWarnings("unchecked")
        List<MethodDescriptor> methods = (List<MethodDescriptor>) context.getMethodDescriptors();

        // Group methods by @Group value
        Map<String, List<MethodDescriptor>> grouped = new HashMap<>();
        List<MethodDescriptor> ungrouped = new ArrayList<>();

        for (MethodDescriptor md : methods) {
            Method m = md.getMethod();
            TestBucket bucketAnno = m.getAnnotation(TestBucket.class);
            if (bucketAnno != null) {
                grouped.computeIfAbsent(bucketAnno.value(), mapper -> new ArrayList<>()).add(md);
            } else {
                ungrouped.add(md);
            }
        }

        // Shuffle within each group
        grouped.values().forEach(Collections::shuffle);
        Collections.shuffle(ungrouped);

        // Shuffle group order itself
        List<List<MethodDescriptor>> buckets = new ArrayList<>(grouped.values());
        buckets.add(ungrouped);
        Collections.shuffle(buckets);

        // Flatten back into execution order
        List<MethodDescriptor> finalOrder = new ArrayList<>();
        for (List<MethodDescriptor> bucket : buckets) {
            finalOrder.addAll(bucket);
        }

        context.getMethodDescriptors().sort(Comparator.comparingInt(finalOrder::indexOf));
    }
}
