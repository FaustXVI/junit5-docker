package com.github.junit5docker;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.github.junit5docker.WaitFor.NOTHING;

/**
 * <p>This annotation is the junit-docker entry point.</p>
 *
 * <p>Adding this annotation to a test's class will start a docker container before running the tests and will be stop
 * at the end of the tests. This is done once per class.</p>
 *
 * @since 1.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerExtension.class)
public @interface Docker {

    /**
     * @return the image's name to start.
     */
    String image();

    /**
     * @return the port mapping to send to docker. This is required since at least one port must be visible for the
     * container to be useful.
     * @see Port
     */
    Port[] ports();

    /**
     * @return the optional environment variables to pass to the docker container.
     * @see Environment
     */
    Environment[] environments() default {};

    /**
     * @return the optional log to wait for before running the tests.
     * @see WaitFor
     */
    WaitFor waitFor() default @WaitFor(NOTHING);

    /**
     * @return true if the container should be recreated for each test case.
     * False if it should be created only once for the test class.
     */
    boolean newForEachCase() default true;
}
