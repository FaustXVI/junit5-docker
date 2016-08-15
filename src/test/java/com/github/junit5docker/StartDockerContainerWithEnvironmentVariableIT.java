package com.github.junit5docker;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8080, inner = 8080),
        environments = {@Environment(key = "test", value = "42"), @Environment(key = "toRead", value = "theAnswer")})
public class StartDockerContainerWithEnvironmentVariableIT {

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
    }

    private void checkConnectionToContainer(int port) {
        try (Socket container = new Socket("localhost", port);
             PrintWriter out = new PrintWriter(container.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(container.getInputStream()))
        ) {
            assertNotNull(container);
            out.println("GET /env HTTP/1.0\n\n");
            List<String> envs = in.lines().collect(Collectors.toList());
            assertTrue(envs.contains("test=42"), "'test' environments variable not found");
            assertTrue(envs.contains("toRead=theAnswer"), "'toRead' environments variable not found");
        } catch (IOException e) {
            fail("The port " + port + " should be listening");
        }
    }
}
