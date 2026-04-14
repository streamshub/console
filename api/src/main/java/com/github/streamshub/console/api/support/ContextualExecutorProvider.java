package com.github.streamshub.console.api.support;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.context.ThreadContext;

@ApplicationScoped
public class ContextualExecutorProvider {

    @Inject
    ExecutorService executor;

    @Inject
    ThreadContext threadContext;

    /**
     * Provide an executor that will run using the server's thread pool, but with
     * the context of the thread calling this method. This allows us to provide
     * request context to the task even though it will run on a thread-pool thread.
     *
     * @return an executor with propagation of the calling thread's context
     */
    public Executor currentContextExecutor() {
        var context = threadContext.currentContextExecutor();
        return task -> executor.execute(() -> context.execute(task));
    }

    /**
     * @see ThreadContext#contextualRunnable(Runnable)
     */
    public Runnable contextualRunnable(Runnable runnable) {
        return threadContext.contextualRunnable(runnable);
    }
}
