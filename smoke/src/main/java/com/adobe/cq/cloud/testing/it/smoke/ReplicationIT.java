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

import java.util.concurrent.TimeUnit;

import com.adobe.cq.cloud.testing.it.smoke.rules.ContentPublishRule;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests to check replication. 
 * 
 * A test page is published from author and publication is 
 * validated through the replication queues on author.
 */
public class ReplicationIT {
    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    @ClassRule
    public static CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    @Rule
    public ContentPublishRule
        contentPublishRule = new ContentPublishRule(root, cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

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
            contentPublishRule.activateAndDeactivate(false);
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Exception e) {
            new Polling(() -> contentPublishRule.activateAndDeactivate(false)).poll(TIMEOUT, 500);
        }
    }
}
