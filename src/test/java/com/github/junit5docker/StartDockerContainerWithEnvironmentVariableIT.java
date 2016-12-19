package com.github.junit5docker;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8080, inner = 8080),
        environments = {@Environment(key = "test", value = "42"), @Environment(key = "toRead", value = "theAnswer")})
public class StartDockerContainerWithEnvironmentVariableIT {

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
        checkConnectionToContainer();
    }

    private void checkConnectionToContainer() {
        try (CloseableHttpResponse container = HttpClientBuilder.create().build()
                .execute(new HttpGet("http://localhost:8080/env"))
        ) {
            assertThat(container).isNotNull();
            String[] envs = EntityUtils.toString(container.getEntity()).split("\\n");
            assertThat(envs).contains("test=42", "toRead=theAnswer");
        } catch (IOException e) {
            fail("The port 8080 should be listening");
        }
    }
}
