package com.github.junit5docker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Docker(image = "faustxvi/simple-two-ports", ports = {@Port(exposed = 8080, inner = 8080), @Port(exposed = 8443, inner =
        8081)})
public class StartDockerContainerWithMultiplePortsIT {

    @BeforeEach
    void verifyContainerIsReady() {
        checkConnectionsToContainer();
    }

    @Test
    void verifyFirstContainerIsStarted() {
        checkConnectionsToContainer();
    }

    @AfterEach
    void verifyContainerIsStillAlive() {
        checkConnectionsToContainer();
    }

    private void checkConnectionsToContainer() {
        checkConnectionToContainer(8080);
        checkConnectionToContainer(8443);
    }

    private void checkConnectionToContainer(int port) {
        try (Socket container = new Socket("localhost", port)) {
            assertNotNull(container);
        } catch (IOException e) {
            fail("The port " + port + " should be listening");
        }
    }
}
