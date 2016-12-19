# language: en
Feature: JUnit5 Docker

[`JUnit5-Docker`](https://github.com/FaustXVI/junit5-docker) is a `JUnit5` extension that start docker containers before running tests and stop them afterwards.
_This is the documentation for the version ${project.version}._

_The last released version can be found at [https://faustxvi.github.io/junit5-docker/current](https://faustxvi.github.io/junit5-docker/current) and the documentation of the under developpment version can be found at [https://faustxvi.github.io/junit5-docker/snapshot](https://faustxvi.github.io/junit5-docker/snapshot)_

Dependency
----------

{% if "${project.version}" contains "SNAPSHOT" %}

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
          <version>${project.version}</version>
          <scope>test</scope>
      </dependency>
```

Usage
-----

  The entrypoint is the `@Docker` annotation.
  Please refer to the [Javadoc](https://faustxvi.github.io/junit5-docker/javadoc/${project.version}) for more details.

  The container is started once for the whole class and before any test method is called; and stopped afterward.

  This mean that the container is already started when the `@BeforeEach` and `@BeforeAll` methods are called and destroyed after the `@AfterEach` and `@AfterAll` methods.

  Be aware that the container is not restarted between tests so changing the state of the container in one test may affect other tests.

  Scenario: Simple Example

  Given that you have a test like :

"""
@Docker(image = "faustxvi/simple-two-ports", ports = @Port(exposed = 8801, inner = 8080))
public class MyAwesomeTest {

    @Test
    void checkMyCode() {
        // Add your test content here
    }

}
"""

  When you run your test :

  * the container "faustxvi/simple-two-ports" is started before running your tests using the version "latest"
  * the port 8801 is bound to the container's port 8080 so you can exchange through this port
  * the container is stopped and removed after your tests


  Scenario: Real life exemple

  Given that you have a test like :

"""
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
"""

 When you run your test :

 * the container is started with the given environment variables
 * the tests are started only after the string "mysqld: ready for connections" is found in the container's logs
 
