# AEM Test Samples
This is a collection of test modules that can be run to validate an AEM-based deployment.
Tests are written according to [Best practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices).

## Modules
* [smoke](./smoke) - generic smoke tests


## How to use
Clone the repository and use maven for running each of the test modules.

The build also produces a `jar-with-dependencies` that can be run as a self-contained test module
(using java directly or a small maven pom with failsafe configured).

### Run the tests against localhost
```bash
mvn clean verify -Ptest-all
```

### Run the test against Skyline author and publish service
The `eaas-local` profile allows to run the test locally but against Skyline author and publish service. The same test client configuration is used like when the test module is executed using EAAS:

```bash
mvn -Peaas-local clean verify \
-Dskyline.author.url=<your-skyline-author-url> \
-Dskyline.author.user=admin \
-Dskyline.author.password=<your-admin-password> \
-Dskyline.publish.url=<your-skyline-publish-url \
-Dskyline.publish.user=admin \
-Dskyline.publish.password=<your-admin-password> \
```

## How to release

 * Commit your changes
 * Get the git short version hash of the commit e.g. `git rev-parse --short HEAD`
 * Create a package: `mvn clean package`
 * Deploy on artifactory:
    ```
    mvn deploy:deploy-file -DgroupId=com.adobe.cq -DartifactId=com.adobe.cq.testing.it.smoke -Dversion=0.1.0-42-<git short version hash> -Dpackaging=jar -Dfile=target/com.adobe.cq.testing.it.smoke-0.1.0-42-SNAPSHOT.jar -DrepositoryId=maven-eaas-release -Durl=https://artifactory-uw2.adobeitc.com/artifactory/maven-eaas-release
    ```