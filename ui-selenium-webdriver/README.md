# UI Selenium Webdriver sample module

Sample structure for Java-based [Selenium](https://www.selenium.dev/) UI test module which conforms to
AEM  Cloud Manager quality gate UI test conventions.


## Cloud Manager UI test module conventions

AEM provides an integrated suite of Cloud Manager quality gates to ensure smooth updates to custom applications,
UI tests are executed as part of a specific quality gate for each Cloud Manager pipeline with a dedicated Custom UI Testing step.

Within the project structure there is a [specific location](https://github.com/adobe/aem-project-archetype/tree/develop/src/main/archetype/ui.tests) 
where the code Custom UI Tests are expected. The code in this folder is used as a Docker build context to produce a docker image
which will be executed during the Custom UI Testing step in the pipeline.

The Cloud Manager UI test module convention defines the expected structure of the test module as well as the environment
variables which will be passed at runtime. This is explained in detail in the [Building UI Tests](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/using-cloud-manager/test-results/ui-testing.html?lang=en#building-ui-tests)
section of the documentation.

## Structure

- `wait-for-grid.sh` Bash script helper to check Selenium readiness in the Docker image
- `Dockerfile` commands to assemble the image, including the maven profile to be executed to run the tests
- `docker-compose-(chrome|firefox).yaml` all-in-one setup to run the test module against AEM instance in the desired browser
- `pom.xml` defines project dependencies and build configuration which will be used by Cloud Manager to build the test module image
- `/test-module` The test project (add your tests there)

## Execute test module

### Locally (standalone)

* Start selenium locally
  ```shell
  # Start selenium docker image (for Linux, Windows, Intel-based Macbooks)
  # we mount /tmp/shared as a shared volume that will be used between selenium and the test module for uploads
  docker run --platform linux/amd64 -d -p 4444:4444 -v /tmp/shared:/tmp/shared  selenium/standalone-chrome-debug:latest
  
  # Start selenium docker image with ARM (for Apple M1/M2 CPUs)
  docker run -d -p 4444:4444 seleniarm/standalone-chromium
  ```
  
* If you don't have docker installed, you can download and run [Selenium Server](https://www.selenium.dev/downloads/) on your machine.

* Change to `test-module` directory and execute, overriding properties as needed
  ```
  mvn verify -Pui-tests-local-execution -DAEM_AUTHOR_URL=https://author.my-deployment.com
  ```
  **NOTE** if running against a local AEM instance (Quickstart) make sure that the AUTHOR/PUBLISH URL are resolvable from
   both the test module and the selenium instance.

## all-in-one

The image built from the Dockerfile can be used to execute tests locally against an AEM environment. The `ui-tests-docker-execution` 
maven profile will start the docker-compose setup starting selenium and the test module, executing the tests against
the AEM instance defined via environment variables. The test results will be stored in the `./target/reports` directory.

The following environment variables (AEM UI test convention) can be passed

| envvar | default               |
| --- |-----------------------|
| AEM_AUTHOR_URL | http://localhost:4502 |
| AEM_AUTHOR_USERNAME | `admin`               |
| AEM_AUTHOR_PASSWORD | `admin`               |
| AEM_PUBLISH_URL | http://localhost:4503 |
| AEM_PUBLISH_USERNAME | `admin`               |
| AEM_PUBLISH_PASSWORD | `admin`               |
| REPORTS_PATH | `target/reports`      |

1. Build the Docker UI test image with below command
   ```
   mvn clean install -Pui-tests-docker-build
   ```
2. Run the test
   ```
   mvn verify -Pui-tests-docker-execution -DAEM_AUTHOR_URL=https://author.my-deployment.com
   ```

## Integrate into your Cloud Manager repository

Follow these steps to use the Java Selenium tests:

1. Remove all content in the `ui.tests` folder from your Cloud Manager repository.

1. Copy all files located in this folder into the `ui.tests` folder.

1. (Optionally) adjust the file `pom.xml` in the current folder and set parent as well as artifact information to the desired naming.

   Note: Do not modify the pom.xml file located inside the `test-module` folder.

1. Commit and push the changes.

During the next pipeline execution, Cloud Manager will use the Java Selenium tests.
