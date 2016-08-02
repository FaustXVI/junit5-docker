package fr.detant;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public class StartDockerContainerTest {

    private static int beforeTestContainerCount;

    private void verifyContainerIsStarted() {
        try (Socket container = new Socket("localhost", 8801)) {
            assertNotNull(container);
        } catch (IOException e) {
            fail("The port 8801 should be listening");
        }
    }

    @BeforeAll
    static void countExistingContainers() {
        beforeTestContainerCount = countContainers();
    }

    @AfterAll
    static void verifyNoContainerAdded() {
        assertEquals(beforeTestContainerCount, countContainers(), "Containers should be remove after execution");
    }

    @Docker(image = "luisbebop/echo-server", ports = "8801:8800")
    @Nested
    public class FirstStartDockerContainerTest {

        @Before
        void verifyContainerIsReady() {
            verifyContainerIsStarted();
        }

        @Test
        void verifyFirstContainerIsStarted() {
            verifyContainerIsStarted();
        }

        @After
        void verifyContainerIsStillAlive() {
            verifyContainerIsStarted();
        }
    }

    @Docker(image = "luisbebop/echo-server", ports = "8801:8800")
    @Nested
    public class SecondStartDockerContainerTest {

        @Test
        void verifySecondContainerIsStarted() {
            verifyContainerIsStarted();
        }
    }

    private static DockerClient dockerClient = DockerClientBuilder.getInstance(
            DockerClientConfig.createDefaultConfigBuilder()
                    .withApiVersion("1.22")
                    .build())
            .build();

    private static int countContainers() {
        return dockerClient.listContainersCmd().withShowAll(true).exec().size();
    }
}
