package com.github.junit5docker;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


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
        try (CloseableHttpResponse container = HttpClientBuilder.create().build()
                .execute(new HttpGet("http://localhost:" + port + "/env"))
        ) {
            assertThat(container).isNotNull();
        } catch (IOException e) {
            fail("The port " + port + " should be listening");
        }
    }
}
