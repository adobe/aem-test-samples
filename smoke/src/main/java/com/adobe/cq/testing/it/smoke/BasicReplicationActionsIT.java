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
package com.adobe.cq.testing.it.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ReplicationClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.*;

import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.http.HttpStatus.SC_OK;

public class BasicReplicationActionsIT {

    private static final long TIMEOUT = MINUTES.toMillis(5);

    @ClassRule
    public static CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    static CQClient adminAuthor;
    static CQClient adminPublish;
    static CQClient anonymousPublish;

    @BeforeClass
    public static void beforeClass() throws ClientException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
        anonymousPublish = cqBaseClassRule.publishRule.getClient(CQClient.class, null, null);
    }

    /**
     * Checks that a GET on the page has the {{expectedStatus}} in the response
     */
    private void checkPage(final int expectedStatus) throws TimeoutException, InterruptedException {
        // verify path is on publish
        new Polling() {
            @Override
            public Boolean call() throws Exception {
                anonymousPublish.doGet(root.getPath() + ".html", expectedStatus);
                return true;
            }
        }.poll(TIMEOUT, 500);
    }

    /**
     * Activates a page as admin, than deletes it. Verifies the deleted page gets removed from publish.
     */
    @Test
    public void testActivate() throws ClientException, InterruptedException, TimeoutException {
        ReplicationClient rClient = adminAuthor.adaptTo(ReplicationClient.class);
        rClient.activate(root.getPath());
        checkPage(200);
    }

    /**
     * Activates a page as admin, than deactivates it. Verifies that the page gets removed from publish.
     */
    @Test
    public void testActivateAndDeactivate() throws ClientException, InterruptedException, TimeoutException {
        ReplicationClient rClient = adminAuthor.adaptTo(ReplicationClient.class);
        rClient.activate(root.getPath());
        checkPage(200);

        rClient.deactivate(root.getPath(), SC_OK);

        // verify that page is not present on publish
        checkPage(404);
    }

    /**
     * Activates a page as admin, than deletes it. Verifies that deleted page gets removed from publish.
     */
    @Test
    public void testActivateAndDelete() throws ClientException, InterruptedException, TimeoutException {
        ReplicationClient rClient = adminAuthor.adaptTo(ReplicationClient.class);
        rClient.activate(root.getPath());
        checkPage(200);

        adminAuthor.deletePage(new String[]{root.getPath()}, false, false);

        // verify that page is not present on publish
        checkPage(404);
    }
}
