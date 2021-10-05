/*
 * Copyright 2021 Adobe Systems Incorporated
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.adobe.cq.cloud.testing.it.smoke.utils.PublishUtils;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ReplicationClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests to check replication
 */
public class ReplicationIT {
    private static Logger log = LoggerFactory.getLogger(ReplicationIT.class);
    
    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);

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

    private PublishUtils publishUtils;

    @BeforeClass
    public static void beforeClass() throws ClientException, IOException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
        anonymousPublish = cqBaseClassRule.publishRule.getClient(CQClient.class, null, null);
        rClient = adminAuthor.adaptTo(ReplicationClient.class);
    }

    @Before
    public void before() throws TimeoutException, InterruptedException {
        publishUtils = new PublishUtils(root, adminAuthor, adminPublish, rClient);
        new Polling(publishUtils::checkContentDistributionAgentExists)
            .poll(TIMEOUT, 500);
    }

    /**
     * Activates a page as admin, then deactivates it.
     * Verifies:
     * <ul>
     *      <li>That the replication queue is empty</li>
     * </ul>
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void testActivateAndDeactivate() throws Exception {

        // This is a workaround for correctly detecting assumption violations.
        // Any AssumptionViolatedException throw by the Callable will be
        // swallowed by the Poller and wrapped in a TimeoutException. As a
        // consequence, the test will be marked as failed instead of skipped.

        try {
            publishUtils.activateAndDeactivate(false);
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Exception e) {
            new Polling(() -> publishUtils.activateAndDeactivate(false)).poll(TIMEOUT, 500);
        }
    }

    /**
     * Activates a page as admin, then deletes it.
     * Verifies:
     * <ul>
     *  <li>That the replication queue is empty</li>
     * </ul>
     * @throws Exception if an error occurred
     */
    @Test
    public void testActivateAndDelete() throws Exception {

        // This is a workaround for correctly detecting assumption violations.
        // Any AssumptionViolatedException throw by the Callable will be
        // swallowed by the Poller and wrapped in a TimeoutException. As a
        // consequence, the test will be marked as failed instead of skipped.

        try {
            publishUtils.activateAndDelete(false);
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Exception e) {
            new Polling(() -> publishUtils.activateAndDelete(false)).poll(TIMEOUT, 500);
        }
    }
}
