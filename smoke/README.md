# AEM Smoke Tests

This is a collection of generic smoke tests to validate the basic functionality of an AEM deployment.

## Run the test against your AEM Cloud Service

For details check the main [README.md](../README.md#run-the-test-against-your-aem-cloud-service-author-and-publish-tiers)
of this repository.

## Exceptions

### Test - PublishEndToEndIT

| Exceptions           | Error Code                  | Reason                                                             |
|----------------------|-----------------------------|--------------------------------------------------------------------|
| PublishException     | PAGE_NOT_AVAILABLE          | Activated page not available on publish                            |
|                      | PAGE_AVAILABLE              | Deactivated page still available on publish                        |
| ReplicationException | QUEUE_BLOCKED               | Replication queue blocked before test                              |
|                      | ACTIVATION_REQUEST_FAILED   | Replication action failed                                          |
|                      | DEACTIVATION_REQUEST_FAILED | Deactivation action failed                                         |
|                      | ACTION_NOT_REPLICATED       | Replication action still pending in agent queues                   |
|                      | REPLICATION_NOT_AVAILABLE   | Replication agents not available before test                       |
| ServiceException     | AUTHOR_NOT_AVAILABLE        | Author is not available before test                                |
|                      | PUBLISH_NOT_AVAILABLE       | Publish is not available before test                               |
| SmokeTestException   | GENERIC                     | Any generic exception. Mostly connection problems with the service |
