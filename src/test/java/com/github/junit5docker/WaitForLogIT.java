package com.github.junit5docker;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Docker(image = "faustxvi/open-port-later", ports = @Port(exposed = 8801, inner = 8080),
        environments = @Environment(key = "WAITING_TIME", value = "1s"),
        waitFor = @WaitFor("started"))
public class WaitForLogIT {

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
        try (CloseableHttpResponse execute = HttpClientBuilder.create().build().execute(new HttpGet("localhost:8801"))) {
            assertNotNull(execute);
        } catch (IOException e) {
            fail("The port 8801 should be listening");
        }
    }
}
