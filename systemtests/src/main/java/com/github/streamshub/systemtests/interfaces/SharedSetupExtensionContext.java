package com.github.streamshub.systemtests.interfaces;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExecutableInvoker;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.MediaType;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class SharedSetupExtensionContext implements ExtensionContext {

    private final ExtensionContext delegate;
    private final String displayNameOverride;

    public SharedSetupExtensionContext(ExtensionContext delegate, String displayNameOverride) {
        this.delegate = delegate;
        this.displayNameOverride = displayNameOverride;
    }

    @Override
    public Optional<ExtensionContext> getParent() {
        return delegate.getParent();
    }

    @Override
    public ExtensionContext getRoot() {
        return delegate.getRoot();
    }

    @Override
    public String getUniqueId() {
        // Slightly tweak uniqueId to include the override
        return delegate.getUniqueId() + "/" + displayNameOverride + "()";
    }

    @Override
    public String getDisplayName() {
        return displayNameOverride;
    }

    @Override
    public Set<String> getTags() {
        return delegate.getTags();
    }

    @Override
    public Optional<AnnotatedElement> getElement() {
        return delegate.getElement();
    }

    @Override
    public Optional<Class<?>> getTestClass() {
        return delegate.getTestClass();
    }

    @Override
    public List<Class<?>> getEnclosingTestClasses() {
        return delegate.getEnclosingTestClasses();
    }

    @Override
    public Class<?> getRequiredTestClass() {
        return delegate.getRequiredTestClass();
    }

    @Override
    public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
        return delegate.getTestInstanceLifecycle();
    }

    @Override
    public Optional<Object> getTestInstance() {
        return delegate.getTestInstance();
    }

    @Override
    public Object getRequiredTestInstance() {
        return delegate.getRequiredTestInstance();
    }

    @Override
    public Optional<TestInstances> getTestInstances() {
        return delegate.getTestInstances();
    }

    @Override
    public TestInstances getRequiredTestInstances() {
        return delegate.getRequiredTestInstances();
    }

    @Override
    public Optional<Method> getTestMethod() {
        return delegate.getTestMethod();
    }

    @Override
    public Method getRequiredTestMethod() {
        return delegate.getRequiredTestMethod();
    }

    @Override
    public Optional<Throwable> getExecutionException() {
        return delegate.getExecutionException();
    }

    @Override
    public Optional<String> getConfigurationParameter(String key) {
        return delegate.getConfigurationParameter(key);
    }

    @Override
    public <T> Optional<T> getConfigurationParameter(String key, Function<String, T> transformer) {
        return delegate.getConfigurationParameter(key, transformer);
    }

    @Override
    public void publishReportEntry(Map<String, String> map) {
        delegate.publishReportEntry(map);
    }

    @Override
    public void publishReportEntry(String key, String value) {
        delegate.publishReportEntry(key, value);
    }

    @Override
    public void publishReportEntry(String value) {
        delegate.publishReportEntry(value);
    }

    @Override
    public void publishFile(String name, MediaType mediaType, ThrowingConsumer<Path> action) {
        // Do nothing
    }

    @Override
    public void publishDirectory(String name, ThrowingConsumer<Path> action) {
        // Do nothing
    }

    @Override
    public Store getStore(Namespace namespace) {
        return delegate.getStore(namespace);
    }

    @Override
    public ExecutionMode getExecutionMode() {
        return delegate.getExecutionMode();
    }

    @Override
    public ExecutableInvoker getExecutableInvoker() {
        return delegate.getExecutableInvoker();
    }
}
