/*
 * Copyright 2022 Adobe Systems Incorporated
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
import java.util.concurrent.TimeoutException;

import com.adobe.cq.cloud.testing.it.smoke.exception.SmokeTestException;
import com.adobe.cq.cloud.testing.it.smoke.rules.ContentPublishRule;
import com.adobe.cq.cloud.testing.it.smoke.rules.ServiceAccessibleRule;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End to end publication test.
 * <p>
 * A test page is published from author and publication is validated through the AEM
 * publish ingress (CDN -&gt; Dispatcher -&gt; AEM publish tier).
 */
public class PublishEndToEndIT {
    private static final Logger LOG = LoggerFactory.getLogger(PublishEndToEndIT.class);

    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(1);
    
    @ClassRule
    public static final CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);
    
    private static final Page root = new Page(cqBaseClassRule.authorRule);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(new ServiceAccessibleRule(cqBaseClassRule.authorRule))
        .around(new ServiceAccessibleRule(cqBaseClassRule.publishRule))
        .around(root);
    
    @Rule
    public ContentPublishRule
        contentPublishRule = new ContentPublishRule(root, cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

    /**
     * Activates a page as admin, then deactivates it.
     * Verifies:
     * <ul>
     *      <li>That the replication queue(s) is empty</li>
     *      <li>After activation, that the page is accessible via the publish ingress</li>
     *      <li>After deactivation, that the page is no longer accessible via the publish ingress and that the queue is empty</li>
     * </ul>
     *
     * @throws InterruptedException if an error occurred
     * @throws TimeoutException if an error occurred
     */
    @Test
    public void testActivateAndDeactivate() throws InterruptedException, TimeoutException {

        // This is a workaround for correctly detecting assumption violations.
        // Any AssumptionViolatedException throw by the Callable will be
        // swallowed by the Poller and wrapped in a TimeoutException. As a
        // consequence, the test will be marked as failed instead of skipped.

        try {
            activateAndDeactivate();
        } catch (AssumptionViolatedException e) {
            LOG.error("Assumption violated", e);
            throw e;
        } catch (SmokeTestException | RuntimeException e) {
            new Polling(this::activateAndDeactivate).poll(TIMEOUT, 500);
        }
    }

    /**
     * Execute activate and deactivate on publish & preview
     *
     * @return true if success
     * @throws SmokeTestException if exception occurs
     */
    private boolean activateAndDeactivate() throws SmokeTestException {
        contentPublishRule.activateAssertPublish();
        contentPublishRule.deactivateAssertPublish();
        
        contentPublishRule.activateAssertPreview();
        contentPublishRule.deactivateAssertPreview();
        return true;
    }
}
