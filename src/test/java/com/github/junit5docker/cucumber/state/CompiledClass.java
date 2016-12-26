package com.github.junit5docker.cucumber.state;

import com.github.junit5docker.Docker;
import com.github.junit5docker.WaitFor;
import com.github.junit5docker.fakes.FakeContainerExtensionContext;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompiledClass {

    private Class<?> compiledClass;

    public void setClass(Class<?> clazz) {
        this.compiledClass = clazz;
    }

    public Class<?> getCompiledClass() {
        return compiledClass;
    }

    public String[] environmentAnnotations() {
        return Stream.of(compiledClass.getAnnotation(Docker.class).environments())
            .map(e -> e.key() + "=" + e.value())
            .collect(Collectors.toList())
            .toArray(new String[]{});
    }

    public List<String> waitAnnotations() {
        return Stream.of(compiledClass.getAnnotation(Docker.class).waitFor())
            .map(WaitFor::value)
            .collect(Collectors.toList());
    }

    public ContainerExtensionContext getExtensionContext() {
        return new FakeContainerExtensionContext(compiledClass);
    }
}
