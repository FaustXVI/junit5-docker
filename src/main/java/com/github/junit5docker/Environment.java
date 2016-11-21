package com.github.junit5docker;

/**
 * Describes an environment variable to set for a docker container.
 *
 * @since 1.0
 */
public @interface Environment {

    /**
     * @return the environment's variable name.
     */
    String key();

    /**
     * @return the environment's variable value.
     */
    String value();
}
