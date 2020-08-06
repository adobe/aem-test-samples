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

### Run the test against your AEM Cloud Service author and publish tiers
The `eaas-local` profile has been added for convenience to allow to run the test locally against an AEM Cloud Service. 
The same test client configuration is used when the test module is executed in the Cloud Service

```bash
mvn -Peaas-local clean verify \
-Dcloud.author.url=<your-aem-author-url> \
-Dcloud.author.user=admin \
-Dcloud.author.password=<your-admin-password> \
-Dcloud.publish.url=<your-aem-publish-url \
-Dcloud.publish.user=admin \
-Dcloud.publish.password=<your-admin-password> \
```
## Requirements

##### User 

The test modules require the `admin` user or an admin-like user with enough privileges to create content, new users, 
groups and replicate content.

##### Replication

The tests also verify author-publish replication therefore for them to work correctly replication needs be be 
configured correctly.


### Sling properties 

The `eaas-local` profile facilitates the definition of sling properties expected by the aem-testing-clients 
(and the underlying Sling Testing Clients) in a convenient way. 

The system properties are as follows:

* sling.it.instances - should be set to 2
* sling.it.instance.url.1 - should be set to the author URL, for example, http://localhost:4502
* sling.it.instance.runmode.1 - should be set to author
* sling.it.instance.adminUser.1 - should be set to the author admin user, e.g. admin
* sling.it.instance.adminPassword.1 - should be set to the author admin password
* sling.it.instance.url.2 - should be set to the author URL, for example, http://localhost:4503
* sling.it.instance.runmode.2 - should be set to publish
* sling.it.instance.adminUser.2 - should be set to the publish admin user, for example, admin
* sling.it.instance.adminPassword.2 - should be set to the publish admin password
* sling.it.configure.default.replication.agents - should be set to false


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
