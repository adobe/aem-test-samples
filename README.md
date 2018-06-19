# AEM Test Samples
This is a collection of test modules that can be run to validate an AEM-based deployment.
Tests are written according to [Best practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices).

## Modules
* [platform](./platform) - generic tests that are testing platform features (e.g. all jsps are compiling)


## How to use
Clone the repository and use maven for running each of the test modules:
```bash
mvn clean verify -Ptest-all
```

The build also produces a `jar-with-dependencies` that can be run as a self-contained test module 
(using java directly or a small maven pom with failsafe configured).

