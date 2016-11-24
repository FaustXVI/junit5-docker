package com.github.junit5docker;

/**
 * Describes a port binding for a docker container.
 *
 * @since 1.0
 */
public @interface Port {

    /**
     * @return the port number exposed on the host.
     * This is the port number to use in the test code for data exchange with the container.
     */
    int exposed();

    /**
     * @return the port number declared by the container.
     * This is the port number used by the application inside the container.
     */
    int inner();
}
