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


@Docker(image = "faustxvi/open-port-later", ports = @Port(exposed = 8801, inner = 8080),
        environments = @Environment(key = "WAITING_TIME", value = "1s"),
        waitFor = @WaitFor("started"))
public class WaitForLogIT {

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
        try (CloseableHttpResponse container = HttpClientBuilder.create().build()
                .execute(new HttpGet("http://localhost:8801"))) {
            assertThat(container).isNotNull();
        } catch (IOException e) {
            fail("The port 8801 should be listening");
        }
    }
}
