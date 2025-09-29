package com.github.streamshub.systemtests.interfaces;

import io.skodjob.testframe.resources.KubeResourceManager;
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


/**
 * A wrapper around JUnit's {@link ExtensionContext} that customizes the
 * unique identifier and display name for tests executed within a
 * {@code @TestBucket}.
 *
 * <p>The main purpose of this class is to act as a dedicated test context
 * for bucketed tests, ensuring that:</p>
 * <ul>
 *   <li>Resources created during {@code @TestBucket} setup are associated
 *       with a unique context separate from standard {@code @BeforeAll}
 *       or per-test contexts.</li>
 *   <li>The {@link KubeResourceManager} can use the modified unique ID to
 *       correctly map resource creation and deletion to the bucket rather
 *       than the individual test case or class context.</li>
 *   <li>All other behaviors of the {@link ExtensionContext} are delegated
 *       to the original context.</li>
 * </ul>
 *
 * <p>The only modifications are:</p>
 * <ul>
 *   <li>{@link #getUniqueId()} — appends a bucket-specific suffix to
 *       isolate resource tracking.</li>
 *   <li>{@link #getDisplayName()} — overridden with the provided display
 *       name for clearer reporting.</li>
 * </ul>
 *
 * <p>This mechanism ensures that shared resources tied to a bucket can
 * be safely created and cleaned up without interfering with unrelated
 * tests or lifecycle contexts.</p>
 */
public class TestBucketExtensionContext implements ExtensionContext {

    private final ExtensionContext delegate;
    private final String displayNameOverride;

    public TestBucketExtensionContext(ExtensionContext delegate, String displayNameOverride) {
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
        // Slightly changed uniqueId to include overriden value
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
        delegate.publishFile(name, mediaType, action);
    }

    @Override
    public void publishDirectory(String name, ThrowingConsumer<Path> action) {
        delegate.publishDirectory(name, action);
    }

    @Override
    public Store getStore(Namespace namespace) {
        return delegate.getStore(namespace);
    }

    @Override
    public Store getStore(StoreScope scope, Namespace namespace) {
        return delegate.getStore(scope, namespace);
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