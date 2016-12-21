package com.github.junit5docker.cucumber;

import com.github.junit5docker.Docker;
import com.github.junit5docker.DockerExtension;
import com.github.junit5docker.WaitFor;
import com.github.junit5docker.fakes.FakeContainerExtensionContext;
import cucumber.api.java8.En;
import org.junit.jupiter.api.extension.ContainerExtensionContext;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class Steps implements En {

    private Containers containers = new Containers();

    private final DockerExtension dockerExtension = new DockerExtension();

    private AtomicReference<Class<?>> testClass = new AtomicReference<>();

    public Steps() {

        Before(() -> {
            containers = new Containers();
        });

        After(() -> {
            containers.verifyAllClean();
        });

        Given("^that you have a test like :$", (String classCode) -> {
            try {
                Path source = createSourceFile(classCode);
                testClass.set(compile(source));
            } catch (IOException | ClassNotFoundException e) {
                fail("Failing because of unexpected exception", e);
            }
        });

        When("^you run your test :$", () -> {
            ContainerExtensionContext context = new FakeContainerExtensionContext(testClass.get());
            dockerExtension.beforeAll(context);
            containers.updateStarted();
            dockerExtension.afterAll(context);
            containers.updateRemainings();
        });

        When("^the container \"([^\"]*)\" is started before running your tests using the version \"([^\"]*)\"$", (String containerName, String version) -> {
            assertThat(containers.startedImageNames()).contains(containerName + ":" + version);
        });

        When("^the port (\\d+) is bound to the container's port (\\d+) so you can exchange through this port$", (Integer outerPort, Integer innerPort) -> {
            assertThat(containers.portMapping()).contains(new Integer[]{outerPort, innerPort});
        });

        When("^the container is stopped and removed after your tests$", () -> {
            assertThat(containers.remainings()).isEmpty();
        });

        When("^the container is started with the given environment variables$", () -> {
            assertThat(containers.environment()).contains(environmentAnnotations());
        });

        When("^the tests are started only after the string \"([^\"]*)\" is found in the container's logs$", (String wantedLog) -> {
            assertThat(waitAnnotations()).overridingErrorMessage("Java code and expectation mismatch on waited log {}", wantedLog).contains(wantedLog);
            assertThat(containers.logs().anyMatch(s -> s.contains(wantedLog))).overridingErrorMessage("Logs should contains {}", wantedLog).isTrue();
        });
    }

    private Class<?> compile(Path source) throws MalformedURLException, ClassNotFoundException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler.run(null, null, System.err, source.toString()) != 0) {
            fail("Fail to compile");
        }

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{source.getParent().toUri().toURL()}, Thread.currentThread().getContextClassLoader());
        return Class.forName("MyAwesomeTest", true, classLoader);
    }

    private Path createSourceFile(String classCode) throws IOException {
        String imports = "import com.github.junit5docker.*;\n" +
                "import org.junit.jupiter.api.*;\n";
        Path directory = Files.createTempDirectory("test");
        Path source = Files.createFile(directory.resolve("MyAwesomeTest.java"));
        Files.write(source, (imports + classCode).getBytes(UTF_8));
        return source;
    }

    private String[] environmentAnnotations() {
        return Stream.of(testClass.get().getAnnotation(Docker.class).environments())
                .map(e -> e.key() + "=" + e.value())
                .collect(Collectors.toList())
                .toArray(new String[]{});
    }


    private List<String> waitAnnotations() {
        return Stream.of(testClass.get().getAnnotation(Docker.class).waitFor())
                .map(WaitFor::value)
                .collect(Collectors.toList());
    }

}
