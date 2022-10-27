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
package com.adobe.cq.cloud.testing.it.xf.smoke.rules;

import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class CleanupRule extends ExternalResource {
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
    public CleanupRule(Instance rule, long timeout, long delay) {
        this.rule = rule;
        this.timeout = timeout;
        this.delay = delay;
    }

    /**
     * Mark a path for deletion at the end of the test enclosed statement
     * @param path path to be deleted
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
}
