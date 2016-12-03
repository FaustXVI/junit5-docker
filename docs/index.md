---
layout: page
title:  JUnit5-docker
category: doc
order: 0
---

# JUnit5-docker

`JUnit5-docker` is a `JUnit5` extension that start docker containers before running tests and stop them afterwards.

## Usage

  The entrypoint is the `@Docker` annotation. Please refer to the [Javadoc]() for more details.
  
  The container is started once for the whole class and before any test method is called; and stopped afterward.
   
  This mean that the container is already started when the `@Before` methods are called and destroyed after the `@After` methods.
  
  Be aware that the container is not restarted between tests so changing the state of the container in one test may affect other tests.
  
### Simple Example
  
  Given that you have a test like : 

```java
@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8801, inner = 8080))
public class MyAwesomeTest {

    @Test
    void checkMyCode() {
        // Add your test content here
    }

}
```

  When you run your test :
  
  * the container "faustxvi/simple-two-ports" is started before running your tests
  * the port 8801 is bound to the container's port 8080 so you can exchange through this port
  * the container is stopped and removed after your tests
 
  
### Real life exemple
  
  Given that you have a test like :

```java
  @Docker(image = "mysql", ports = @Port(exposed = 8801, inner = 3306),
          environments = {
                  @Environment(key = "MYSQL_ROOT_PASSWORD", value = "root"),
                  @Environment(key = "MYSQL_DATABASE", value = "testdb"),
                  @Environment(key = "MYSQL_USER", value = "testuser"),
                  @Environment(key = "MYSQL_PASSWORD", value = "s3cr3t"),
          },
          waitFor = @WaitFor("mysqld: ready for connections"))
  public class MyAwesomeTest {
  
      @Test
      void checkMyCode() {
          // Add your test content here
      }
  
  }
```

 When you run your test :
 
 * the container is started with the given environment variables
 * the tests are started only after the string "started" is found in the container's logs
 
