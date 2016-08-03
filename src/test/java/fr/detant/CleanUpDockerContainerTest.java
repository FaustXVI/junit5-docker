package fr.detant;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CleanUpDockerContainerTest {

    private static int beforeTestContainerCount;

    @BeforeAll
    static void countExistingContainers() {
        beforeTestContainerCount = countContainers();
    }

    @AfterAll
    static void verifyNoContainerAdded() {
        assertEquals(beforeTestContainerCount, countContainers(), "Containers should be remove after execution");
    }

    @Docker(image = "luisbebop/echo-server", ports = @Port(exposed = 8801, inner = 8800))
    @Nested
    public class FirstStartDockerContainerTest {

        @Test
        void aTestThatUseTheContainer() {
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
