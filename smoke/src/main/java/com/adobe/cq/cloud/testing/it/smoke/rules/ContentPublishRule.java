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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ReplicationClient;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.codehaus.jackson.JsonNode;
import org.junit.AssumptionViolatedException;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class ContentPublishRule extends ExternalResource {
    private Logger log = LoggerFactory.getLogger(Page.class);

    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    protected static final long TIMEOUT_PER_TRY = TimeUnit.MINUTES.toMillis(1);

    protected static final String DIST_AGENTS_PATH = "/libs/sling/distribution/services/agents";
    protected static final String PUBLISH_DIST_AGENT = "publish";

    protected static final String PUB_QUEUES_PATH = DIST_AGENTS_PATH + "/" + PUBLISH_DIST_AGENT;

    private final Page root;

    private final Instance authorRule;
    private final Instance publishRule;

    private ReplicationClient replicationClient;

    private boolean previewAvailable;
    private CQClient publishClient;
    private CQClient authorClient;

    public ContentPublishRule(Page root, Instance authorRule, Instance publishRule) {
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
    }

    /**
     * Execute activate and delete on publish and preview if available
     * 
     * @param isCheckPage if to assert on the presence/absence of the page on publish
     * @return true if success
     * @throws Exception if exception occurs
     */
    public boolean activateAndDelete(boolean isCheckPage) throws Exception {
        activateAssertPublish(isCheckPage);

        deleteAssertPublish(isCheckPage);
        return true;
    }

    /**
     * Execute activate and deactivate on publish and preview if available
     *
     * @param isCheckPage if to assert on the presence/absence of the page on publish
     * @return true if success
     * @throws Exception if exception occurs
     */
    public boolean activateAndDeactivate(boolean isCheckPage) throws Exception {
        activateAssertPublish(isCheckPage);

        deactivateAssertPublish(isCheckPage);
        return true;
    }

    private void waitPublishQueueEmptyOfPath() throws Exception {
        new Polling(() -> waitQueueEmptyOfPath(PUB_QUEUES_PATH, root.getPath()))
            .poll(TIMEOUT, 500);
    }
    
    /**
     * Checks if the item is empty or if not whether the replicated path is in the list of packages.
     * Sample curl response
     * <pre>
     * {
     *   "name": "publish",
     *   "queues": {
     *     "items": [
     *       "7ece8e2c-d641-4d50-a0ff-6fcd3c52f7d8-publishSubscriber",
     *       "0c77809c-cb61-437b-981b-0424c042fd92-publishSubscriber"
     *     ],
     *     "7ece8e2c-d641-4d50-a0ff-6fcd3c52f7d8-publishSubscriber": {
     *       "state": "BLOCKED",
     *       "items": [
     *         "package-0@52734825"
     *       ],
     *       "itemsCount": 1,
     *       "empty": false,
     *       "package-0@52734825": {
     *         "size": 6073,
     *         "paths": [
     *           "/content/test-site/testpage_8e57dec8-40e8-4e42-a267-ff5142f5c472"
     *         ],
     *         "errorMessage": "Failed attempt (12/infinite) to import the distribution package"
     *     },
     *     "0c77809c-cb61-437b-981b-0424c042fd92-publishSubscriber": {
     *       "sling:resourceType": "sling/distribution/service/agent/queue",
     *       "state": "IDLE",
     *       "items": [],
     *       "itemsCount": 0,
     *       "empty": true
     *     }
     *   },
     *   "log": {
     *     "sling:resourceType": "sling/distribution/service/log"
     *   },
     *   "status": {
     *     "state": "BLOCKED"
     *   }
     * }
     * </pre> 
     *
     * @param agentPath the agent path for which queues to be asserted
     * @param replicatedPath the path replicated
     * @return
     * @throws Exception
     */
    private boolean waitQueueEmptyOfPath(String agentPath, String replicatedPath) throws Exception {
        JsonNode queuesJson = replicationClient.doGetJson(agentPath, 2, 200, 300).get("queues");
        log.debug("queuesJson for agentPath {} is {}", agentPath, queuesJson);
        Set<String> queueIds = elementsAsText(queuesJson.get("items"));

        for (String queueId : queueIds) {
            JsonNode queueJson = queuesJson.get(queueId);

            boolean isEmpty = queueJson.get("empty").getBooleanValue();
            log.debug("Queue {} is empty {}", queueId, isEmpty);

            if (!isEmpty) {
                Set<String> pkgs = elementsAsText(queueJson.get("items"));
                for (String pkg : pkgs) {
                    JsonNode pkgJson = queueJson.get(pkg);

                    if (pkgJson != null) {
                        Set<String> paths = elementsAsText(pkgJson.get("paths"));

                        if (paths.contains(replicatedPath)) {
                            log.warn("Package [{}] in queue [{}] with paths {} failed due to [{}]", pkg, queueId,
                                paths, pkgJson.get("errorMessage"));
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }
    
    private static Set<String> elementsAsText(JsonNode queue) {
        return elements(queue).map(JsonNode::getTextValue).collect(Collectors.toSet());
    }

    private static Stream<JsonNode> elements(JsonNode node) {
        if (node == null ) {
            return Stream.empty();
        }
        Iterator<JsonNode> elementsIt = node.getElements();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(elementsIt, Spliterator.ORDERED), false);
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
        return checkContentDistributionAgentExists(PUBLISH_DIST_AGENT);
    }
    
    /**
     * Checks whether the given agent exists.
     * 
     * @param agent to check
     * @return true if the given agent exists
     * @throws ClientException if an error occurred
     */
    private Boolean checkContentDistributionAgentExists(String agent) throws ClientException {
        JsonNode agents = getAuthorClient().doGetJson(DIST_AGENTS_PATH, 3);
        log.info("Replication agents list: {}", agents);
        if (agents.path(agent).isMissingNode()) {
            log.warn("Default distribution agent {} is missing from the distribution list", agent);
            return false;
        }
        return true;
    }
    
    private void activateAssertPublish(boolean isCheckPage) throws Exception {
        replicationClient.activate(root.getPath());
        waitPublishQueueEmptyOfPath();

        if (isCheckPage) {
            checkPage(SC_OK);
        }
    }
    
    private void deactivateAssertPublish(boolean isCheckPage) throws Exception {
        replicationClient.deactivate(root.getPath());
        waitPublishQueueEmptyOfPath();

        if (isCheckPage) {
            checkPage(SC_NOT_FOUND);
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

