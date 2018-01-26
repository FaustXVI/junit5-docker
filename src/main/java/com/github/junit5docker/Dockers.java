package com.github.junit5docker;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation is a junit-docker entry point.</p>
 * <p>It can be used to start multiple docker containers per test.</p>
 * <p>
 * <p>It must contain one or more {@link com.github.junit5docker.Docker} annotations.</p>
 *
 * @see Docker
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerExtension.class)
public @interface Dockers {

    /**
     * @return the list of Docker containers to start.
     * @see Docker
     */
    Docker[] value();

}
