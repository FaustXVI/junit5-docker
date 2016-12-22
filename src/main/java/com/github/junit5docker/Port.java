package com.github.junit5docker;

/**
 * Describes a port binding for a docker container.
 *
 * @since 1.0
 */
public @interface Port {

    /**
     * This is the port number to use in the test code for data exchange with the container.
     *
     * @return the port number exposed on the host.
     */
    int exposed();

    /**
     * This is the port number used by the application inside the container.
     *
     * @return the port number declared by the container.
     */
    int inner();
}
