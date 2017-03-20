---
layout: page
title: JUnit5 Docker
category: doc
order: 0
---

  [`JUnit5-Docker`](https://github.com/FaustXVI/junit5-docker) is a `JUnit5` extension that start docker containers before running tests and stop them afterwards.
  _This is the documentation for the version 1.0.0-SNAPSHOT._

  _The last released version can be found at [https://faustxvi.github.io/junit5-docker/current](https://faustxvi.github.io/junit5-docker/current) and the documentation of the under developpment version can be found at [https://faustxvi.github.io/junit5-docker/snapshot](https://faustxvi.github.io/junit5-docker/snapshot)_

  Dependency
  ----------

  {% if "1.0.0-SNAPSHOT" contains "SNAPSHOT" %}

  Since this is a SNAPSHOT version, you can't find it on maven central so you'll need to add the repository to your `pom.xml` like this :

  ```xml
  <repositories>
  <repository>
  <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
  <id>ossrh</id>
  </repository>
  </repositories>
  ```

  {% endif %}

  To use `JUnit5-Docker`, you first need to declare it as a dependency. Add these lines to your `pom.xml` if you are using maven.

  ```xml
  <dependency>
  <groupId>com.github.faustxvi</groupId>
  <artifactId>junit5-docker</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>test</scope>
  </dependency>
  ```

  Usage
  -----

  The entrypoint is the `@Docker` annotation.
  Please refer to the [Javadoc](https://faustxvi.github.io/junit5-docker/javadoc/1.0.0-SNAPSHOT) for more details.

  The container is started before each test method is called; and stopped afterward.

  This mean that the container is already started when the `@BeforeEach` methods are called and destroyed after the `@AfterEach` methods.

  By default, you are guaranteed that a new container is created for each tests.

### Simple Example

    Given that you have a test like :

```java
@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8801, inner = 8080))
public class MyAwesomeTest {

    @Test
    void checkMyCode() {
        // Add your test content here
    }

    @Test
    void checkMyCodeWithAnotherContainer() {
        // Add your test content here
    }

}
```

    When you run your tests :

    * a new container `faustxvi/simple-two-ports` is started before running each tests using the version `latest`
    * the port `8801` is bound to the container's port `8080` so you can exchange through this port
    * this container is stopped and removed after usage

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

    When you run your tests :

    * the container is started with the given environment variables
    * the tests are started only after the string `mysqld: ready for connections` is found in the container's logs

### Keep the container for all tests of a class

  The container is started once for the whole class and before any test method is called; and stopped afterward.

  This mean that the container is already started when the `@BeforeEach` and `@BeforeAll` methods are called and destroyed after the `@AfterEach` and `@AfterAll` methods.

  Be aware that the container is not restarted between tests so changing the state of the container in one test may affect other tests.

    Given that you have a test like :

```java
@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8801, inner = 8080), newForEachCase = false)
public class MyAwesomeTest {

    @Test
    void checkMyCode() {
        // Add your test content here
    }

    @Test
    void checkMyCodeWithTheSameContainer() {
        // Add your test content here
    }

}
```

    When you run your tests :

    * the container `faustxvi/simple-two-ports` is started before running your tests using the version `latest`
    * the container is stopped and removed after your tests
