# Selenium Java Sample Test Module

This module documents the recommended structure for a Java Selenium test module and adheres to the Cloud Manager UI test module conventions,
ensuring that tests will be executed and reports generated are stored in the proper location.

Some examples of basic tasks like logging in-out of AEM instances, taking screenshots, logging browser requests are included.


## Usage

### local testing

- Start selenium locally
  ```shell
  # Start selenium docker image (for Linux, Windows, Intel-based Macbooks)
  # we mount /tmp/shared as a shared volume that will be used between selenium and the test module for uploads
  docker run --platform linux/amd64 -d -p 4444:4444 -v /tmp/shared:/tmp/shared selenium/standalone-chrome-debug:latest
  
  # Start selenium docker image with ARM (for Apple M1/M2 CPUs)
  docker run -d -p 4444:4444 -v /tmp/shared:/tmp/shared seleniarm/standalone-chromium
  ```

* If you don't have docker installed, you can download and run [Selenium Server](https://www.selenium.dev/downloads/) on your machine.

Select a profile:
- `ui-tests-local-execution`  provides some common default properties for running local that can be overriden on the command line, eg:
   ```
   mvn verify -Pui-tests-local-execution  -DAEM_AUTHOR_URL="<AUTHOR_URL>"
   ```
   **NOTE** if running against a local AEM instance (Quickstart) make sure that the AUTHOR/PUBLISH URL are resolvable from
   both the test module and the selenium instance.
 
- `ui-tests-cloud-execution` expects that all values required by convention are passed as environment variables.
   This is the profile which is executed during CM tests in this sample module
   ```
   mvn verify -Pui-tests-cloud-execution
   ```


### Reports

The reports produced by the failsafe plugin are saved in the directory specified by the REPORTS_PATH parameter. 
This sample module includes a step that generates HTML reports using the surefire-report-plugin, utilizing the output from the XML report as the source.

`$REPORTS_PATH/html_report` will contain the HTML report.

### Screenshots 

A sample TestRule `FailureScreenShotRule` is included which illustrates how a screenshot can be taken in case of test failure.
````java
@Rule
public FailureScreenShotRule failure = new FailureScreenShotRule(driver);
````
 
`$REPORTS_PATH/screenshots` will contain the images.

### Browser logs

The test module configures the Selenium driver to be able to capture browser logs and demonstrates how the logs can be 
dumped after a test execution and appended to the test report by using the following TestRule

```java
@Rule
public BrowserLogsDumpRule browserLogs =  new BrowserLogsDumpRule(driver);
```
example output 
```
***** Browser logs *****
Thu Feb 09 12:58:32 CET 2023 SEVERE https://example.com/foo - Failed to load resource: the server responded with a status of 403 ()

```





