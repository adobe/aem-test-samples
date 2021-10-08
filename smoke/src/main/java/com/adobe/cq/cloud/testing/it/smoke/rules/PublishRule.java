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
package com.adobe.cq.cloud.testing.it.smoke.rules;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ReplicationClient;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.AssumptionViolatedException;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.cq.testing.client.ReplicationClient.DIST_AGENTS_PATH;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class PublishRule extends ExternalResource {
    private Logger log = LoggerFactory.getLogger(Page.class);

    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    protected static final long TIMEOUT_PER_TRY = TimeUnit.MINUTES.toMillis(1);
    
    protected static final String PUBLISH_DIST_AGENT = "publish";
    protected static final String PREVIEW_DIST_AGENT = "preview";

    protected static final String PUB_QUEUES_PATH = DIST_AGENTS_PATH + "/" + PUBLISH_DIST_AGENT;
    protected static final String PREVIEW_QUEUES_PATH = DIST_AGENTS_PATH + "/" + PREVIEW_DIST_AGENT;

    private final Page root;

    private final Instance authorRule;
    private final Instance publishRule;

    private ReplicationClient replicationClient;

    private boolean previewAvailable;
    private CQClient publishClient;
    private CQClient authorClient;

    public PublishRule(Page root, Instance authorRule, Instance publishRule) {
        this.root = root;
        this.authorRule = authorRule;
        this.publishRule = publishRule;
    }

    /**
     * Initialize the replication client
     * Assert publish agent is available
     * Check if preview agent exists
     * 
     * @throws Exception if exception occurs
     */
    @Override
    protected void before() throws Exception {
        replicationClient = getAuthorClient().adaptTo(ReplicationClient.class);
        
        try {
            new Polling(this::checkContentDistributionPublishAgentExists).poll(TIMEOUT, 500);
        } catch (Exception e) {
            log.error("Unable to assert publish agent", e);
            throw e;
        }
        try {
            this.previewAvailable = checkContentDistributionPreviewAgentExists();
        } catch (Exception e) {
            log.info("No preview agent found", e);
        }
    }

    /**
     * Execute activate and delete on publish and preview if available
     * 
     * @param isCheckPage if to assert on the presence/absence of the page on publish
     * @return
     * @throws Exception if exception occurs
     */
    public boolean activateAndDelete(boolean isCheckPage) throws Exception {
        activateAssertPublish(isCheckPage);
        activateAssertPreview();

        deleteAssertPublish(isCheckPage);
        waitPreviewQueueEmptyOfPath();

        return true;
    }

    /**
     * Execute activate and deactivate on publish and preview if available
     *
     * @param isCheckPage if to assert on the presence/absence of the page on publish
     * @return
     * @throws Exception if exception occurs
     */
    public boolean activateAndDeactivate(boolean isCheckPage) throws Exception {
        activateAssertPublish(isCheckPage);
        activateAssertPreview();

        deactivateAssertPublish(isCheckPage);
        deactivateAssertPreview();

        return true;
    }

    private void waitPublishQueueEmptyOfPath() throws Exception {
        new Polling(() -> replicationClient.waitQueueEmptyOfPath(PUB_QUEUES_PATH, root.getPath()))
            .poll(TIMEOUT, 500);
    }

    private void waitPreviewQueueEmptyOfPath() throws Exception {
        new Polling(() -> replicationClient.waitQueueEmptyOfPath(PREVIEW_QUEUES_PATH, root.getPath()))
            .poll(TIMEOUT, 500);
    }

    private void checkPage(final int... expectedStatus) throws Exception {
        checkPage(true, expectedStatus);
    }
    
    /**
     * Checks that a GET on the page on publish has the {{expectedStatus}} in the response
     *
     * @throws Exception if an error occurred
     */
    private void checkPage(boolean skipDispatcherCache, final int...  expectedStatus) throws Exception {
        final String path = root.getPath() + ".html";
        log.info("Checking page {} returns status {}", getPublishClient().getUrl(path), expectedStatus);
        SlingHttpResponse res = null;
        final List<NameValuePair> queryParams = skipDispatcherCache
            ? Collections.singletonList(
            new BasicNameValuePair("timestamp", String.valueOf(System.currentTimeMillis())))
            : Collections.emptyList();

        res = getPublishClient().doGet(path, queryParams, Collections.emptyList());
        if (null != res && res.getStatusLine().getStatusCode() == SC_UNAUTHORIZED) {
            throw new AssumptionViolatedException("Publish requires auth for (SAML?). Skipping...");
        }

        try {
            new Polling() {
                @Override public Boolean call() throws Exception {
                    final List<NameValuePair> queryParams = skipDispatcherCache ?
                        Collections.singletonList(
                            new BasicNameValuePair("timestamp", String.valueOf(System.currentTimeMillis()))) :
                        Collections.emptyList();
                    getPublishClient().doGet(path, queryParams, Collections.emptyList(), expectedStatus);
                    return true;
                }
            }.poll(TIMEOUT_PER_TRY, 1000);
        } catch (TimeoutException te) {
            log.warn("Checking page {} with expected status {} failed. Please check that the connectivity to {} is proper",
                root.getPath(), expectedStatus, getPublishClient().getUrl());
            throw te;
        }
    }

    private Boolean checkContentDistributionPublishAgentExists() throws ClientException {
        return replicationClient.checkContentDistributionAgentExists(PUBLISH_DIST_AGENT);
    }

    private Boolean checkContentDistributionPreviewAgentExists() throws ClientException {
        return replicationClient.checkContentDistributionAgentExists(PREVIEW_DIST_AGENT);
    }
    
    private void activateAssertPublish(boolean isCheckPage) throws Exception {
        replicationClient.activate(root.getPath());
        waitPublishQueueEmptyOfPath();

        if (isCheckPage) {
            checkPage(SC_OK);
        }
    }

    private void activateAssertPreview() throws Exception {
        if (previewAvailable) {
            replicationClient.activate(PREVIEW_DIST_AGENT, root.getPath());
            waitPreviewQueueEmptyOfPath();
        }
    }
    
    private void deactivateAssertPublish(boolean isCheckPage) throws Exception {
        replicationClient.deactivate(root.getPath());
        waitPublishQueueEmptyOfPath();

        if (isCheckPage) {
            checkPage(SC_NOT_FOUND);
        }
    }

    private void deactivateAssertPreview() throws Exception {
        if (previewAvailable) {
            replicationClient.deactivate(PREVIEW_DIST_AGENT, root.getPath());
            waitPreviewQueueEmptyOfPath();
        }
    }
    
    private void deleteAssertPublish(boolean isCheckPage) throws Exception {
        getAuthorClient().deletePage(new String[]{root.getPath()}, true, false);
        waitPublishQueueEmptyOfPath();
        if (isCheckPage) {
            checkPage(SC_NOT_FOUND);
        }
    }
    
    /**
     * The client to use to create and delete this page. The default implementation creates a {@link CQClient}.
     * The default implementation also uses the default admin user.
     *
     * @return The client to use to create and delete this page.
     */
    protected CQClient getPublishClient() {
        if (publishClient == null) {
            publishClient = publishRule.getAdminClient(CQClient.class);
        }
        return publishClient;
    }

    /**
     * The author client to use to create and delete this page. The default implementation creates a {@link CQClient}.
     * The default implementation also uses the default admin user.
     *
     * @return The client to use to create and delete this page.
     */
    protected CQClient getAuthorClient() {
        if (authorClient == null) {
            authorClient =  authorRule.getAdminClient(CQClient.class);
        }
        return authorClient;
    }
}

