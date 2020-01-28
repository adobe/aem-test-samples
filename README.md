# AEM Test Samples
This is a collection of test modules that can be run to validate an AEM cloud-based deployment.
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
-Dcloud.author.url=<your-skyline-author-url> \
-Dcloud.author.user=admin \
-Dcloud.author.password=<your-admin-password> \
-Dcloud.publish.url=<your-skyline-publish-url \
-Dcloud.publish.user=admin \
-Dcloud.publish.password=<your-admin-password> \
```

## How to deploy pre-release (maintainers only)

 * Commit your changes
 * Get the git short version hash of the commit e.g. `git rev-parse --short HEAD`
 * Create a package: `mvn clean package`
 * Deploy on artifactory:
    ```
    mvn deploy:deploy-file -DgroupId=com.adobe.cq.cloud -DartifactId=com.adobe.cq.cloud.testing.it.smoke -Dversion=0.1.0-<git short version hash> -Dpackaging=jar -Dfile=target/com.adobe.cq.cloud.testing.it.smoke-0.1.0-SNAPSHOT.jar -DrepositoryId=maven-eaas-release -Durl=https://artifactory-uw2.adobeitc.com/artifactory/maven-eaas-release
    ```
   
## Notable Examples

* [Creating a page with the admin user](./smoke/src/main/java/com/adobe/cq/cloud/testing/it/smoke/CreatePageAdminIT.java)

* [Creating a page with a transient author user](./smoke/src/main/java/com/adobe/cq/cloud/testing/it/smoke/CreatePageAsAuthorUserIT.java)
