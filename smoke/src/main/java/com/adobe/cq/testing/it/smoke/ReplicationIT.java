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

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ReplicationClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;

public class ReplicationIT {
    private Logger log = LoggerFactory.getLogger(ReplicationIT.class);

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(120);

    @ClassRule
    public static CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    static CQClient adminAuthor;
    static CQClient adminPublish;
    static CQClient anonymousPublish;

    private static ReplicationClient rClient;

    @BeforeClass
    public static void beforeClass() throws ClientException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
        anonymousPublish = cqBaseClassRule.publishRule.getClient(CQClient.class, null, null);
        rClient = adminAuthor.adaptTo(ReplicationClient.class);
    }

    /**
     * Activates a page as admin, then deactivates it. Verifies that the page gets removed from publish.
     */
    @Test
    public void testActivateAndDeactivate() throws ClientException, InterruptedException, TimeoutException {
        rClient.activate(root.getPath());
        checkPage(SC_OK);

        rClient.deactivate(root.getPath(), SC_OK);
        checkPage(SC_NOT_FOUND);
    }

    /**
     * Activates a page as admin, than deletes it. Verifies that deleted page gets removed from publish.
     */
    @Test
    public void testActivateAndDelete() throws ClientException, InterruptedException, TimeoutException {
        rClient.activate(root.getPath());
        checkPage(SC_OK);

        adminAuthor.deletePage(new String[]{root.getPath()}, false, false);
        checkPage(SC_NOT_FOUND);
    }
    
    /**
     * Checks that a GET on the page on publish has the {{expectedStatus}} in the response
     */
    private void checkPage(final int expectedStatus) throws TimeoutException, InterruptedException {
        final String path = root.getPath() + ".html";
        log.info("Checking page {} returns status {}", anonymousPublish.getUrl(path), expectedStatus);
        new Polling() {
            @Override
            public Boolean call() throws Exception {
                anonymousPublish.doGet(path, expectedStatus);
                return true;
            }
        }.poll(TIMEOUT, 500);
    }

}
