package fr.detant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ContainerExtensionContext;
import org.mockito.Mockito;

public class DockerExtensionTest {

    private DockerClientAdapter dockerClient = Mockito.mock(DockerClientAdapter.class);

    private DockerExtension dockerExtension = new DockerExtension(dockerClient);

    @Nested
    class BeforeAllTestsShould {

        @Test
        public void startContainerWithOnePort() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.beforeAll(context);
            Mockito.verify(dockerClient).startContainer("wantedImage", new PortBinding(8801, 8800));
        }

        @Test
        public void startContainerWithMultiplePorts() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(MultiplePortTest.class);
            dockerExtension.beforeAll(context);
            Mockito.verify(dockerClient).startContainer("wantedImage", new PortBinding(8801, 8800), new PortBinding(9901,
                    9900));
        }
    }

    @Nested
    class AfterAllTestsShould {

        private static final String CONTAINER_ID = "CONTAINER_ID";

        @BeforeEach
        public void callBefore() throws Exception {
            Mockito.when(dockerClient.startContainer(Mockito.anyString(), Mockito.any(PortBinding[].class)))
            .thenReturn(CONTAINER_ID);
            dockerExtension.beforeAll(new FakeContainerExtensionContext(OnePortTest.class));
        }

        @Test
        public void stopContainer() throws Exception {
            ContainerExtensionContext context = new FakeContainerExtensionContext(OnePortTest.class);
            dockerExtension.afterAll(context);
            Mockito.verify(dockerClient).stopAndRemoveContainer(CONTAINER_ID);
        }
    }

    @Docker(image = "wantedImage", ports = @Port(exposed = 8801, inner = 8800))
    private static class OnePortTest {
    }

    @Docker(image = "wantedImage", ports = {@Port(exposed = 8801, inner = 8800), @Port(exposed = 9901, inner = 9900)})
    private static class MultiplePortTest {
    }
}