package com.github.junit5docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DockerClientBuilder;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.dockerjava.core.DefaultDockerClientConfig.createDefaultConfigBuilder;
import static com.github.junit5docker.StartMultipleDockerAnnotationsIT.TEST_NETWORK_1;
import static com.github.junit5docker.StartMultipleDockerAnnotationsIT.TEST_NETWORK_2;
import static org.assertj.core.api.Assertions.assertThat;

@Dockers({
    @Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8081, inner = 8080),
        networks = {TEST_NETWORK_1, TEST_NETWORK_2}),
    @Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8082, inner = 8080),
        networks = {TEST_NETWORK_1, TEST_NETWORK_2})})
@DisplayName("Multiple docker containers with multiple networks")
class StartMultipleDockerAnnotationsIT {

    static final String TEST_NETWORK_1 = "testNetwork1";

    static final String TEST_NETWORK_2 = "testNetwork2";

    @BeforeAll
    static void verifyCleanStateBefore() {
        assertNothingRunning();
    }

    @BeforeEach
    void verifyContainerIsReady() {
        assertContainersRunning();
    }

    @Test
    void verifyFirstContainerIsStarted() {
        assertContainersRunning();
    }

    @AfterEach
    void verifyContainerIsStillAlive() {
        assertContainersRunning();
    }

    @AfterAll
    static void verifyCleanStateAfter() {
        assertNothingRunning();
    }

    private static void assertNothingRunning() {
        assertThat(canConnectOnPort(8081)).isFalse();
        assertThat(canConnectOnPort(8082)).isFalse();
        assertThat(getNetworks(TEST_NETWORK_1)).isEmpty();
        assertThat(getNetworks(TEST_NETWORK_2)).isEmpty();
    }

    private static void assertContainersRunning() {
        assertThat(canConnectOnPort(8081)).isTrue();
        assertThat(canConnectOnPort(8082)).isTrue();
        assertContainersConnectedToNetwork(TEST_NETWORK_1);
        assertContainersConnectedToNetwork(TEST_NETWORK_2);
    }

    private static void assertContainersConnectedToNetwork(String networkName) {
        List<Network> networks1 = getNetworks(networkName);

        assertThat(networks1).size().isEqualTo(1);
        Network network = networks1.get(0);
        assertThat(network.getContainers()).size().isEqualTo(2);
        assertThat(network.getName()).isEqualTo(networkName);
    }

    private static List<Network> getNetworks(String networkName) {
        DockerClient dockerClient = DockerClientBuilder.getInstance(createDefaultConfigBuilder().withApiVersion("1.22"))
                                                       .build();
        return dockerClient.listNetworksCmd()
                           .exec()
                           .stream()
                           .filter(n -> networkName.equals(n.getName()))
                           .collect(Collectors.toList());
    }

    private static boolean canConnectOnPort(int port) {
        HttpGet request = new HttpGet("http://localhost:" + port + "/env");
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
             CloseableHttpResponse response = httpClient.execute(request)) {
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException e) {
            return false;
        }
    }
}
