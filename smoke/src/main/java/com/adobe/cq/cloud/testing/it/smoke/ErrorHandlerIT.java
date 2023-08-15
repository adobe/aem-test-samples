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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * This test class is protecting from flawed AEM error handler implementations that are not returning 404s in case
 * of page not found.
 */
public class ErrorHandlerIT {

    private static CQClient adminAuthor;
    private static CQClient adminPublish;


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
     * Ensure the author returns a 404 when requesting a non-exising page.
     *
     * @throws ClientException
     */
    @Test
    public void testAuthorResponseCode404() throws ClientException {
        adminAuthor.doGet("/content/does/not/exist.html", 404);
    }

    /**
     * Ensure the publish returns a 404 when requesting a non-existing page.
     *
     * @throws ClientException
     */
    @Test
    public void testPublishResponseCode404() throws ClientException {
        adminPublish.doGet("/content/does/not/exist.html", 404);
    }
}
