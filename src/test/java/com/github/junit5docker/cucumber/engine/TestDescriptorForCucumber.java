package com.github.junit5docker.cucumber.engine;

import com.github.junit5docker.cucumber.state.Containers;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.hierarchical.Node;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class TestDescriptorForCucumber implements InvocationHandler {

    private final TestDescriptor testDescriptor;

    private final Containers containers;

    private TestDescriptorForCucumber(TestDescriptor testDescriptor, Containers containers) {
        this.testDescriptor = testDescriptor;
        this.containers = containers;
    }

    public Set<? extends TestDescriptor> getChildren() {
        return testDescriptor.getChildren().stream()
            .map((discover1) -> (TestDescriptor) createTestDescriptorForCucumber(discover1, containers))
            .collect(Collectors.toSet());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            Method[] declaredMethods = TestDescriptorForCucumber.class.getDeclaredMethods();
            Optional<Method> foundedMethod =
                Stream.of(declaredMethods)
                    .filter(m -> method.getName().equals(m.getName()))
                    .findFirst();
            if (foundedMethod.isPresent()) {
                return foundedMethod.get().invoke(this, args);
            } else {
                return method.invoke(testDescriptor, args);
            }
        } finally {
            if ("before".equals(method.getName())) containers.updateStarted();
            if ("after".equals(method.getName())) containers.updateRemainings();
        }
    }

    static TestDescriptor createTestDescriptorForCucumber(TestDescriptor testDescriptor, Containers containers) {
        return (TestDescriptor) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
            new Class[]{TestDescriptor.class, Node.class}, new TestDescriptorForCucumber(testDescriptor, containers));

    }

}
