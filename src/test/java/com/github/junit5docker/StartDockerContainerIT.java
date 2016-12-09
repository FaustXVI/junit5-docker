package com.github.junit5docker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8801, inner = 8080))
public class StartDockerContainerIT {

    @BeforeEach
    void verifyContainerIsReady() {
        checkConnectionToContainer();
    }

    @Test
    void verifyFirstContainerIsStarted() {
        checkConnectionToContainer();
    }

    @AfterEach
    void verifyContainerIsStillAlive() {
        checkConnectionToContainer();
    }

    private void checkConnectionToContainer() {
        try (Socket container = new Socket("localhost", 8801)) {
            assertThat(container).isNotNull();
        } catch (IOException e) {
            fail("The port 8801 should be listening");
        }
    }
}
