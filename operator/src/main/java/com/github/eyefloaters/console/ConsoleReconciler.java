package com.github.eyefloaters.console;

import java.util.Collections;
import java.util.Map;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

@ControllerConfiguration
public class ConsoleReconciler implements EventSourceInitializer<Console>, Reconciler<Console>, Cleaner<Console> {

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Console> context) {
        // TODO Auto-generated method stub
        return Collections.emptyMap();
    }

    @Override
    public DeleteControl cleanup(Console resource, Context<Console> context) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UpdateControl<Console> reconcile(Console resource, Context<Console> context) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
