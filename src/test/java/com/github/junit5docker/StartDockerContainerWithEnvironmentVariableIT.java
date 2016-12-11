package com.github.junit5docker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

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
        checkConnectionToContainer(8080);
    }

    private void checkConnectionToContainer(int port) {
        try (Socket container = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(container.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(container.getInputStream()))
        ) {
            assertThat(container).isNotNull();
            out.println("GET /env HTTP/1.0\n\n");
            List<String> envs = in.lines().collect(Collectors.toList());
            assertThat(envs).contains("test=42", "toRead=theAnswer");
        } catch (IOException e) {
            fail("The port " + port + " should be listening");
        }
    }
}
