package com.github.junit5docker.fakes;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeExtensionContext implements ExtensionContext {
    private final Class<?> testSampleClass;

    public FakeExtensionContext(Class<?> testSampleClass) {
        this.testSampleClass = testSampleClass;
    }

    @Override
    public Optional<ExtensionContext> getParent() {
        return null;
    }

    @Override
    public ExtensionContext getRoot() {
        return null;
    }

    @Override
    public String getUniqueId() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public Set<String> getTags() {
        return null;
    }

    @Override
    public Optional<AnnotatedElement> getElement() {
        return null;
    }

    @Override
    public Optional<Class<?>> getTestClass() {
        return Optional.of(testSampleClass);
    }

    @Override
    public Optional<Object> getTestInstance() {
        return null;
    }

    @Override
    public Optional<Method> getTestMethod() {
        return null;
    }

    @Override
    public Optional<Throwable> getExecutionException() {
        return null;
    }

    @Override
    public void publishReportEntry(Map<String, String> map) {
    }

    @Override
    public Store getStore(Namespace namespace) {
        return null;
    }
}
