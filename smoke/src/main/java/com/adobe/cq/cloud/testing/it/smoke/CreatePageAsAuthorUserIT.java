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
import com.adobe.cq.testing.client.security.CQAuthorizableManager;
import com.adobe.cq.testing.client.security.CQPermissions;
import com.adobe.cq.testing.client.security.Group;
import com.adobe.cq.testing.client.security.User;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MINUTES;


public class CreatePageAsAuthorUserIT {

    private static final String CONTENT_NODE = "/content";
    private static final Logger LOG = LoggerFactory.getLogger(CreatePageAsAuthorUserIT.class);
    private static final int TIMEOUT = (int) MINUTES.toMillis(2);

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    private CQAuthorizableManager authorizableManager;
    private CQClient adminAuthor;
    private CQClient authorAuthor;

    private CQSecurityClient sClient;
    private CQPermissions permissionsObj;

    private User testUser;
    private Group testGroup;
    private CQClient testUserClient;


    @Before
    public void setupSecurityBase() throws ClientException, InterruptedException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        authorAuthor = cqBaseClassRule.authorRule.getClient(CQClient.class, "author", "author");
        sClient = cqBaseClassRule.authorRule.getAdminClient(CQSecurityClient.class);
        permissionsObj = new CQPermissions(sClient);
        authorizableManager = sClient.getManager();

        String authorizableId = createUniqueAuthorizableId("testUser");
        testUser = sClient.createUser(authorizableId, authorizableId, null, null, false, null, 201);
        testGroup = sClient.createGroup(createUniqueAuthorizableId("testGroup"), 201);
        // Add the new user to this group
        testGroup.addMembers(new User[]{testUser});

        // set permissions
        permissionsObj.changePermissions(testGroup.getId(), CONTENT_NODE, true, true, true, false,
                false, false, false, 200);
        // Login as testuser
        testUserClient = new CQClient(sClient.getUrl(), testUser.getId(), testUser.getId());
    }

    @After
    public void cleanupAuthorizables() throws ClientException {
        if (testUser != null && testUser.exists()) {
            testUser.delete(200);
        }

        if (testGroup != null && testGroup.exists()) {
            testGroup.delete(200);
        }
    }

    /**
     * Create unique authorizable Id to make sure no side effects from versioning / restore occurs
     *
     * @param authorizableId authorizable id
     * @return unique authorizableId
     */
    public static String createUniqueAuthorizableId(String authorizableId) {
        return authorizableId + new Date().getTime();
    }

    /**
     * Verifies that a user belonging to the "Authors" group can create a page
     *
     * @throws InterruptedException if the wait was interrupted
     * @throws ClientException if an error occurred
     */
    @Test
    public void testCreatePageAsAuthor() throws InterruptedException, ClientException {
        String pageName = "testpage_" +  UUID.randomUUID();
        String pagePath = "/content/" + pageName;
        try {
            SlingHttpResponse response = testUserClient.createPageWithRetry(pageName, "Page created by CreatePageAsAuthorUserIT",
                    "/content", "", MINUTES.toMillis(1), 500, HttpStatus.SC_OK);
            pagePath = response.getSlingLocation();
            LOG.info("Created page at {}", pagePath);

            // This shows that it exists for the author user
            Assert.assertTrue(String.format("Page %s not created within %s timeout", pagePath, TIMEOUT),
                    testUserClient.pageExistsWithRetry(pagePath, TIMEOUT));
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
