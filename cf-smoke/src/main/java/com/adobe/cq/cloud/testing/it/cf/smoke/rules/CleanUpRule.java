/*
 * Copyright 2019 Adobe Systems Incorporated
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

package com.adobe.cq.cloud.testing.it.cf.smoke.rules;

import com.adobe.cq.testing.client.CQClient;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CleanUpRule extends ExternalResource {

    private static final Logger LOG = LoggerFactory.getLogger(CleanUpRule.class);

    private static final ThreadLocal<List<String>> toDelete = ThreadLocal.withInitial(() -> new ArrayList<>(15));
    private final Instance rule;
    private final long timeout;
    private final long delay;

    /**
     * Cleanup all the added paths via #addPath() at the end of the test enclosed statement
     * @param rule The CQAuthorPublishClassRule
     * @param timeout milliseconds timeout for deleting one path
     * @param delay delay in milliseconds in between retries to delete a path
     */
    public CleanUpRule(Instance rule, long timeout, long delay) {
        this.rule = rule;
        this.timeout = timeout;
        this.delay = delay;
    }

    /**
     * Mark a path for deletion at the end of the test enclosed statement
     * @param path path to be deleted at the end
     */
    public void addPath(String path) {
        toDelete.get().add(path);
    }

    @Override
    protected void after() {
        toDelete.get().forEach((path) -> {
            try {
                new Polling(() -> {
                    rule.getAdminClient().deletePath(path);
                    return rule.getAdminClient().exists(path);
                }).poll(timeout, delay);
            } catch (InterruptedException | TimeoutException | RuntimeException ignored) {}
        });
    }

    /**
     * Generic helper method to clean up remaining folders.
     *
     * @param rule - the instance rules to be used for CQClients
     * @param path - the path desired to be discarded
     * @param timeout - the timeout for the delete path request
     * @param delay - the delay at which to call the delete path request
     *
     * @throws TimeoutException if the cleanup is not successful before timeout
     * @throws InterruptedException to mark this method as "waiting"
     */
    public static void cleanUp(Instance rule, String path, long timeout, long delay) throws TimeoutException, InterruptedException {
        new Polling(() -> {
            LOG.debug("Specifically cleaning up the path: " + path);
            rule.getAdminClient(CQClient.class).deletePath(path);
            return rule.getAdminClient(CQClient.class).exists(path);
        }).poll(timeout, delay);
    }
}

