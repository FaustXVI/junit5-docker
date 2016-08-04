package fr.detant;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Docker(image = "nginx", ports = {@Port(exposed = 8080, inner = 80), @Port(exposed = 8443, inner = 443)})
public class StartDockerContainerWithMultiplePortsTest {

    @Before
    void verifyContainerIsReady() {
        checkConnectionsToContainer();
    }

    @Test
    void verifyFirstContainerIsStarted() {
        checkConnectionsToContainer();
    }

    @After
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
