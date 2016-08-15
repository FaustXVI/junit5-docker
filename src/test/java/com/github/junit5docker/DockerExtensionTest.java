package com.github.junit5docker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;

public class DockerExtensionTest {

    private DockerClientAdapter dockerClient = Mockito.mock(DockerClientAdapter.class);

    private DockerExtension dockerExtension = new DockerExtension(dockerClient);

    @Nested
    class BeforeAllTestsShould {

        @Test
        public void startContainerWithOnePort() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(), eq(new PortBinding(8801, 8800)));
        }

        @Test
        public void startContainerWithMultiplePorts() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(MultiplePortTest.class);
            dockerExtension.beforeAll(context);
            verify(dockerClient).startContainer(eq("wantedImage"), anyMap(), eq(new PortBinding(8801, 8800)),
                    eq(new PortBinding(9901, 9900)));
        }

        @Test
        public void startContainerWithEnvironmentVariables() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OneEnvironmentTest.class);
            dockerExtension.beforeAll(context);
            ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dockerClient).startContainer(eq("wantedImage"),
                    mapArgumentCaptor.<String, String>capture(), any(PortBinding[].class));
            Map<String,String> environment = mapArgumentCaptor.getValue();
            assertEquals(1,environment.size());
            assertTrue(environment.containsKey("toTest"));
            assertEquals("myValue",environment.get("toTest"));
        }
    }

    @Nested
    class AfterAllTestsShould {

        private static final String CONTAINER_ID = "CONTAINER_ID";

        @BeforeEach
        public void callBefore() throws Exception {
            Mockito.when(dockerClient.startContainer(Mockito.anyString(), Mockito.<String, String>anyMap(),
                    any(PortBinding[].class)))
                    .thenReturn(CONTAINER_ID);
            dockerExtension.beforeAll(new FakeContainerExtensionContext(OnePortTest.class));
        }

        @Test
        public void stopContainer() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.afterAll(context);
            verify(dockerClient).stopAndRemoveContainer(CONTAINER_ID);
        }
    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800))
    private static class OnePortTest {
    }

    @Docker(image = "wantedImage", ports = {@Port(exposed = 8801, inner = 8800), @Port(exposed = 9901, inner = 9900)})
    private static class MultiplePortTest {
    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800),
            environments = @Environment(key = "toTest", value = "myValue"))
    private static class OneEnvironmentTest {
    }
}