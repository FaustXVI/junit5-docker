package fr.detant;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@Docker(image = "luisbebop/echo-server", ports = "8801:8800")
public class StartDockerContainerTest {

    @Test
    void shouldRunADockerContainer() {
        try (Socket container = new Socket("localhost", 8801)) {
            assertNotNull(container);
        } catch (IOException e) {
            fail("The port 8801 should be listening");
        }
    }
}
