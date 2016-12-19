package com.github.junit5docker.cucumber;

import cucumber.api.java8.En;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.fail;

public class CompilationSteps implements En {

    public CompilationSteps(CompiledClass compiledClass) {

        Given("^that you have a test like :$", (String classCode) -> {
            try {
                Path source = createSourceFile(classCode);
                compiledClass.setClass(compile(source));
            } catch (IOException | ClassNotFoundException e) {
                fail("Failing because of unexpected exception", e);
            }
        });

    }

    private Class<?> compile(Path source) throws MalformedURLException, ClassNotFoundException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler.run(null, null, System.err, source.toString()) != 0) {
            fail("Fail to compile");
        }

        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{source.getParent().toUri().toURL()},
            Thread.currentThread().getContextClassLoader());
        return Class.forName("MyAwesomeTest", true, classLoader);
    }

    private Path createSourceFile(String classCode) throws IOException {
        String imports = "import com.github.junit5docker.*;\n"
            + "import org.junit.jupiter.api.*;\n";
        Path directory = Files.createTempDirectory("test");
        Path source = Files.createFile(directory.resolve("MyAwesomeTest.java"));
        Files.write(source, (imports + classCode).getBytes(UTF_8));
        return source;
    }

}
