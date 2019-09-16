package com.adobe.cq.cloud.testing.it.xf.smoke.rules;

import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;

import java.util.ArrayList;
import java.util.List;

public class CleanupRule extends ExternalResource {
    public ThreadLocal<List<String>> toDelete = ThreadLocal.withInitial(() -> new ArrayList<>(15));
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
     * @param path
     */
    public void addPath(String path) {
        toDelete.get().add(path);
    }

    @Override
    protected void after() {
        toDelete.get().stream().forEach((path) -> {
            try {
                new Polling(() -> {
                    rule.getAdminClient().deletePath(path);
                    return rule.getAdminClient().exists(path);
                }).poll(timeout, delay);
            } catch (Throwable t) {
            }
        });
    }
}
