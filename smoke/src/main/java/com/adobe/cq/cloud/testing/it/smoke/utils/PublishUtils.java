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
package com.adobe.cq.cloud.testing.it.smoke.utils;

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
import org.codehaus.jackson.JsonNode;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class PublishUtils {
    private Logger log = LoggerFactory.getLogger(Page.class);

    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    protected static final long TIMEOUT_PER_TRY = TimeUnit.MINUTES.toMillis(1);

    protected static final String DIST_AGENTS_PATH = "/libs/sling/distribution/services/agents";
    protected static final String PUBLISH_DIST_AGENT = "publish";
    protected static final String QUEUES_PATH = DIST_AGENTS_PATH + "/" + PUBLISH_DIST_AGENT;
    
    private final CQClient authorClient;
    private final CQClient publishClient;
    private final ReplicationClient replicationClient;

    private final Page root;

    public PublishUtils(Page root, CQClient authorClient, CQClient publishClient, ReplicationClient replicationClient) {
        this.root = root;
        this.authorClient = authorClient;
        this.publishClient = publishClient;
        this.replicationClient = replicationClient;
    }

    public void checkPage(final int... expectedStatus) throws Exception {
        checkPage(true, expectedStatus);
    }
    
    /**
     * Checks that a GET on the page on publish has the {{expectedStatus}} in the response
     *
     * @throws Exception if an error occurred
     */
    public void checkPage(boolean skipDispatcherCache, final int...  expectedStatus) throws Exception {
        final String path = root.getPath() + ".html";
        log.info("Checking page {} returns status {}", publishClient.getUrl(path), expectedStatus);
        SlingHttpResponse res = null;
        final List<NameValuePair> queryParams = skipDispatcherCache
            ? Collections.singletonList(
            new BasicNameValuePair("timestamp", String.valueOf(System.currentTimeMillis())))
            : Collections.emptyList();

        res = publishClient.doGet(path, queryParams, Collections.emptyList());
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
                    publishClient.doGet(path, queryParams, Collections.emptyList(), expectedStatus);
                    return true;
                }
            }.poll(TIMEOUT_PER_TRY, 1000);
        } catch (TimeoutException te) {
            log.warn("Checking page {} with expected status {} failed. Please check that the connectivity to {} is proper",
                root.getPath(), expectedStatus, publishClient.getUrl());
            throw te;
        }
    }

    public Boolean checkContentDistributionAgentExists() throws ClientException {
        JsonNode agents = authorClient.doGetJson(DIST_AGENTS_PATH, 3);
        log.info("Replication agents list: {}", agents);
        if (agents.path(PUBLISH_DIST_AGENT).isMissingNode()) {
            log.warn("Default distribution agent {} is missing from the distribution list", PUBLISH_DIST_AGENT);
            return false;
        }
        return true;
    }

    public void waitPublishQueueEmptyOfPath() throws Exception {
        new Polling(() -> waitQueueEmptyOfPath(PUBLISH_DIST_AGENT, root.getPath()))
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
     * @param agentName the agent name for which queues to be asserted
     * @param replicatedPath the path replicated
     * @return
     * @throws Exception
     */
    public boolean waitQueueEmptyOfPath(String agentName, String replicatedPath) throws Exception {
        JsonNode queuesJson = replicationClient.doGetJson(QUEUES_PATH, 2, 200, 300).get("queues");
        log.debug("queuesJson is {}", queuesJson);
        Set<String> queueIds = elementsAsText(queuesJson.get("items"));

        for (String queueId : queueIds) {
            JsonNode queueJson = queuesJson.get(queueId);

            boolean isEmpty = queueJson.get("empty").getBooleanValue();
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
                    log.debug("Queue {} is empty {}", queueId, isEmpty);
                    return false;
                }
            }
        }
        return true;
    }


    public boolean activateAndDelete(boolean isCheckPage) throws Exception {
        replicationClient.activate(root.getPath());
        waitPublishQueueEmptyOfPath();
        if (isCheckPage) {
            checkPage(SC_OK);
        }

        authorClient.deletePage(new String[]{root.getPath()}, true, false);
        waitPublishQueueEmptyOfPath();
        if (isCheckPage) {
            checkPage(SC_NOT_FOUND);
        }

        return true;
    }

    public boolean activateAndDeactivate(boolean isCheckPage) throws Exception {
        replicationClient.activate(root.getPath());
        waitPublishQueueEmptyOfPath();
        if (isCheckPage) {
            checkPage(SC_OK);
        }

        replicationClient.deactivate(root.getPath(), SC_OK);
        waitPublishQueueEmptyOfPath();
        if (isCheckPage) {
            checkPage(SC_NOT_FOUND);
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
}

