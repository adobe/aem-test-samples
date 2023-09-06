/*
 * Copyright 2023 Adobe Systems Incorporated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.cq.cloud.testing.it.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import org.apache.sling.testing.clients.ClientException;
import org.junit.AssumptionViolatedException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * This test class is protecting from flawed AEM error handler implementations that are not returning 404s in case
 * of page not found.
 */
public class ErrorHandlerIT {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlerIT.class);

    private static CQClient adminAuthor;
    private static CQClient adminPublish;


    private static final String testPage = "/content/test-site/missingpage_%s.html";
    private static final String testErrorMessage = new StringBuilder()
            .append("Error handler test on %s failed. Getting a %s response code when requesting the non-existing resource '%s'.")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("This test explicitly asserts that requesting non-existing resources respond with a 404. If this is not ")
            .append("the case, this usually indicates to a flawed AEM error handler. If you use a custom error handler ")
            .append("make sure it responds with a 404, see https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/developing/full-stack/custom-error-page.html?lang=en")
            .append(System.lineSeparator())
            .append(System.lineSeparator()).toString();


    /**
     * Initializing author and publish class rules.
     */
    @ClassRule
    public static final CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    /**
     * Initialize new author and publish clients for use by the tests.
     */
    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
    }

    /**
     * Ensure the author returns a 404 when requesting a random non-exising page.
     *
     * @throws ClientException
     */
    @Test
    public void testAuthorResponseCode404() throws ClientException {
        String path = String.format(testPage, UUID.randomUUID());
        try {
            adminAuthor.doGet(path, 404);
        } catch (ClientException e) {
            handleAuthorClientException(e, path);
        }
    }

    /**
     * Ensure the publish returns a 404 when requesting a random non-existing page.
     *
     * @throws ClientException
     */
    @Test
    public void testPublishResponseCode404() throws ClientException {
        String path = String.format(testPage, UUID.randomUUID());
        try {
            adminPublish.doGet(path, 404);
        } catch (ClientException e) {
            handlePublishClientException(e, path);
        }
    }

    private void handlePublishClientException(ClientException e, String path) throws ClientException {
        log.error(String.format(testErrorMessage, "publish", e.getHttpStatusCode(), path));
        handleClientException(e);
    }

    private void handleAuthorClientException(ClientException e, String path) throws ClientException {
        log.error(String.format(testErrorMessage, "author", e.getHttpStatusCode(), path));
        handleClientException(e);
    }

    private void handleClientException(ClientException e) throws ClientException {
        // Skip the test in case of 401 or 403, usually related to dispatcher customizations like custom authentication or
        // restricted access to /content/test-site, to be removed once CQ-4345438 got addressed
        if (e.getHttpStatusCode() == SC_UNAUTHORIZED || e.getHttpStatusCode() == SC_FORBIDDEN) {
            throw new AssumptionViolatedException("Skipping test...");
        } else {
            throw e;
        }
    }
}
