# junit5-docker

[![Build Status](https://travis-ci.org/FaustXVI/junit5-docker.svg?branch=master)](https://travis-ci.org/FaustXVI/junit5-docker)

Start docker containers from your junit tests

# Example

```java
@Docker(image = "nginx", ports = {@Port(exposed = 8080, inner = 80), @Port(exposed = 8443, inner = 443)})
public class StartNginxIT {
    @Test
    void shouldTestSomething(){
        ...
    }
}
```

Before any test of this class is run, junit5-docker starts a nginx container and bind the ports so you can use it in 
your tests.