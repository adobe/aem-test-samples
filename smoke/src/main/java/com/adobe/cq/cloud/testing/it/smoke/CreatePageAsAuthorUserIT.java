/*
 * Copyright 2018 Adobe Systems Incorporated
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
import com.adobe.cq.testing.client.CQSecurityClient;
import com.adobe.cq.testing.client.security.CQPermissions;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import com.adobe.cq.testing.junit.rules.TemporaryContentAuthorGroup;
import com.adobe.cq.testing.junit.rules.TemporaryUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.exceptions.TestingIOException;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;

public class CreatePageAsAuthorUserIT {

    private static final Logger LOG = LoggerFactory.getLogger(CreatePageAsAuthorUserIT.class);

    private static final int TIMEOUT = (int) MINUTES.toMillis(2);

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    private static final CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    // Create a random page so the test site is initialized properly.
    private final Page temporaryPage = new Page(cqBaseClassRule.authorRule);
    
    private static final TemporaryContentAuthorGroup groupRule = new TemporaryContentAuthorGroup(cqBaseClassRule.authorRule::getAdminClient);

    @Rule
    public TestRule cqRuleChainGroup = RuleChain.outerRule(cqBaseRule).around(groupRule);

    private static final TemporaryUser userRule = new TemporaryUser(cqBaseClassRule.authorRule::getAdminClient, groupRule.getGroupName());

    @Rule
    public TestRule cqRuleChain = RuleChain.outerRule(cqBaseRule).around(temporaryPage).around(userRule);

    /**
     * Verifies that a user belonging to the "Authors" group can create a page
     *
     * @throws InterruptedException if the wait was interrupted
     * @throws ClientException if an error occurred
     */
    @Test
    public void testCreatePageAsAuthor() throws InterruptedException, ClientException {
        String pageName = "testpage_" +  UUID.randomUUID();
        String pagePathExpected = temporaryPage.getParentPath() + "/" + pageName;
        String pagePath = pagePathExpected;

        try (
                SlingHttpResponse response = createTestPage(pageName, 1)
        ) {
            assert response != null;
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                AssumptionViolatedException e = new AssumptionViolatedException("Author User " + userRule.getClient().getUser() + " not authorized to create page. Skipping...");
                LOG.error("Unable to create test page", e);
                throw e;
            }
            pagePath = response.getSlingLocation();
            if (StringUtils.isEmpty(pagePath)) {
                pagePath = response.getSlingPath();
            }
            String expectedResponseLike = "[...] <a href=\""  + pagePathExpected +"\" id=\"Location\">" + pagePathExpected + "</a> [...]";
            assertFalse("Not able to get created page path from sling response. Expected response like " + expectedResponseLike, StringUtils.isEmpty(pagePath));
            LOG.info("Created page at {}", pagePath);

            // This shows that it exists for the author user
            Assert.assertTrue(String.format("Page %s not created within %s timeout", pagePath, TIMEOUT),
                    userRule.getClient().pageExistsWithRetry(pagePath, TIMEOUT));
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of page creation", e);
        } finally {
            try {
                cqBaseClassRule.authorRule.getAdminClient().adaptTo(CQClient.class)
                        .deletePageWithRetry(pagePath, true, false, 2000, 500);
                LOG.info("Deleted page at {}", pagePath);
            } catch (ClientException e) {
                LOG.error("Unable to delete the page", e);
            }
        }
    }

    private SlingHttpResponse createTestPage(String pageName, int authRetries) throws ClientException, InterruptedException {
        // update the test group permissions in case test page path is blocked for "everyone" group
        Supplier<SlingClient> creatorSupplier = cqBaseClassRule.authorRule::getAdminClient;
        CQSecurityClient securityClient = creatorSupplier.get().adaptTo(CQSecurityClient.class);
        CQPermissions permissionsObj = new CQPermissions(securityClient);
        permissionsObj.changePermissions(groupRule.getGroupName(), temporaryPage.getParentPath(),
                true, true, true, false, false, false, false,
                HttpStatus.SC_OK);

        // create test page
        SlingHttpResponse response = userRule.getClient().createPageWithRetry(
                pageName,
                "Page created by CreatePageAsAuthorUserIT",
                temporaryPage.getParentPath(),
                "",
                MINUTES.toMillis(1),
                500,
                HttpStatus.SC_OK,
                HttpStatus.SC_UNAUTHORIZED
        );

        // retry in case not fully synced on time
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED && authRetries > 0) {
            LOG.info("Got response status {} while creating page {}, retrying...", HttpStatus.SC_UNAUTHORIZED, pageName);
            SECONDS.sleep(5);
            return createTestPage(pageName, --authRetries);
        }

        return response;
    }
}
