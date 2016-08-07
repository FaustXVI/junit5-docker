package fr.detant;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Docker(image = "emilevauge/whoami", ports = @Port(exposed = 8801, inner = 80))
public class StartDockerContainerIT {

    @Before
    void verifyContainerIsReady() {
        checkConnectionToContainer();
    }

    @Test
    void verifyFirstContainerIsStarted() {
        checkConnectionToContainer();
    }

    @After
    void verifyContainerIsStillAlive() {
        checkConnectionToContainer();
    }

    private void checkConnectionToContainer() {
        try (Socket container = new Socket("localhost", 8801)) {
            assertNotNull(container);
        } catch (IOException e) {
            fail("The port 8801 should be listening");
        }
    }
}
