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
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import com.adobe.cq.testing.junit.rules.TemporaryContentAuthorGroup;
import com.adobe.cq.testing.junit.rules.TemporaryUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertFalse;

public class CreatePageAsAuthorUserIT {

    private static final Logger LOG = LoggerFactory.getLogger(CreatePageAsAuthorUserIT.class);

    private static final int TIMEOUT = (int) MINUTES.toMillis(2);

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    // Create a random page so the test site is initialized properly.
    private final Page temporaryPage = new Page(cqBaseClassRule.authorRule);
    
    public TemporaryContentAuthorGroup groupRule = new TemporaryContentAuthorGroup(() -> cqBaseClassRule.authorRule.getAdminClient());

    @Rule
    public TestRule cqRuleChainGroup = RuleChain.outerRule(cqBaseRule).around(groupRule);

    public TemporaryUser userRule = new TemporaryUser(() -> cqBaseClassRule.authorRule.getAdminClient(), groupRule.getGroupName());

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
        try {
            SlingHttpResponse response = userRule.getClient().createPageWithRetry(pageName, "Page created by CreatePageAsAuthorUserIT",
                    temporaryPage.getParentPath(), "", MINUTES.toMillis(1), 500, HttpStatus.SC_OK);
            if (null != response && response.getStatusLine().getStatusCode() == SC_UNAUTHORIZED) {
                throw new AssumptionViolatedException("Author User " + userRule.getClient().getUser() + " not authorized to create page. Skipping...");
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
}
