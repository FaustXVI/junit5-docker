package com.github.junit5docker;

import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class FakeContainerExtensionContext implements ContainerExtensionContext {
    private Class<?> testSampleClass;

    public FakeContainerExtensionContext(Class<?> testSampleClass) {
        this.testSampleClass = testSampleClass;
    }

    @Override
    public Optional<ExtensionContext> getParent() {
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
    public Optional<Method> getTestMethod() {
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
