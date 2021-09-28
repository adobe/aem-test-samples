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
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.codehaus.jackson.JsonNode;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * End to end publication test
 */
public class PublishEndToEndIT {
    private static Logger log = LoggerFactory.getLogger(PublishEndToEndIT.class);

    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    protected static final long TIMEOUT_PER_TRY = TimeUnit.MINUTES.toMillis(1);

    protected static final String DIST_AGENTS_PATH = "/libs/sling/distribution/services/agents";
    protected static final String PUBLISH_DIST_AGENT = "publish";
    protected static final String QUEUES_PATH = DIST_AGENTS_PATH + "/" + PUBLISH_DIST_AGENT;

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
    public static void beforeClass() throws ClientException, IOException {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
        anonymousPublish = cqBaseClassRule.publishRule.getClient(CQClient.class, null, null);
        rClient = adminAuthor.adaptTo(ReplicationClient.class);
    }

    @Before
    public void before() throws TimeoutException, InterruptedException {
        new Polling(this::checkContentDistributionAgentExists)
        	.poll(TIMEOUT, 500);
    }

	/**
     * Activates a page as admin, then deactivates it.
     * Verifies:
     * <ul>
     *      <li>That the replication queue is empty</li>
     *      <li>If check enabled verifies that deleted page gets removed from publish</li>
     * </ul>
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void testActivateAndDeactivate() throws Exception {

        // This is a workaround for correctly detecting assumption violations.
        // Any AssumptionViolatedException throw by the the Callable will be
        // swallowed by the Poller and wrapped in a TimeoutException. As a
        // consequence, the test will be marked as failed instead of skipped.

        try {
            activateAndDeactivate();
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Exception e) {
            new Polling(this::activateAndDeactivate).poll(TIMEOUT, 500);
        }
    }
    
    /**
     * Activates a page as admin, then deletes it.
     * Verifies:
     * <ul>
     *  <li>That the replication queue is empty</li>
     *  <li>If check enabled verifies that deleted page gets removed from publish</li>
     * </ul>
     * @throws Exception if an error occurred
     */
    @Test
    public void testActivateAndDelete() throws Exception {

        // This is a workaround for correctly detecting assumption violations.
        // Any AssumptionViolatedException throw by the the Callable will be
        // swallowed by the Poller and wrapped in a TimeoutException. As a
        // consequence, the test will be marked as failed instead of skipped.

        try {
            activateAndDelete();
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Exception e) {
            new Polling(this::activateAndDelete).poll(TIMEOUT, 500);
        }
    }

    protected boolean activateAndDelete() throws Exception {
        rClient.activate(root.getPath());
        isPublishQueueEmptyOfPath(root.getPath());
        if (isPageCheckEnabled()) {
            checkPage(SC_OK);
        }
        
        adminAuthor.deletePage(new String[]{root.getPath()}, true, false);
        isPublishQueueEmptyOfPath(root.getPath());
        if (isPageCheckEnabled()) {
            checkPage(SC_NOT_FOUND);
        }        
        return true;
    }

    protected boolean activateAndDeactivate() throws Exception {
        rClient.activate(root.getPath());
        isPublishQueueEmptyOfPath(root.getPath());
        if (isPageCheckEnabled()) {
            checkPage(SC_OK);
        }
        rClient.deactivate(root.getPath(), SC_OK);
        isPublishQueueEmptyOfPath(root.getPath());
        if (isPageCheckEnabled()) {
            checkPage(SC_NOT_FOUND);
        }
        
	    return true;
	}
    
	/**
     * Checks that a GET on the page on publish has the {{expectedStatus}} in the response
     *
     * @throws Exception if an error occurred
     */
    private void checkPage(boolean skipDispatcherCache, final int...  expectedStatus) throws Exception {
        final String path = root.getPath() + ".html";
        log.info("Checking page {} returns status {}", adminPublish.getUrl(path), expectedStatus);
        SlingHttpResponse res = null;
        final List<NameValuePair> queryParams = skipDispatcherCache
                ? Collections.singletonList(
                new BasicNameValuePair("timestamp", String.valueOf(System.currentTimeMillis())))
                : Collections.emptyList();

        res = adminPublish.doGet(path, queryParams, Collections.emptyList());
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
                    adminPublish.doGet(path, queryParams, Collections.emptyList(), expectedStatus);
                    return true;
                }
            }.poll(TIMEOUT_PER_TRY, 1000);
        } catch (TimeoutException te) {
            log.warn("Checking page {} with expected status {} failed. Please check that the connectivity to {} is proper", 
                root.getPath(), expectedStatus, adminPublish.getUrl());
            throw te;
        }
    }

    private void checkPage(final int... expectedStatus) throws Exception {
        checkPage(true, expectedStatus);
    }

    /**
     * Returns true if the page check is to be done.
     * @return true or false
     */
    protected boolean isPageCheckEnabled() {
        return true;
    }

	private Boolean checkContentDistributionAgentExists() throws ClientException {
		JsonNode agents = adminAuthor.doGetJson(DIST_AGENTS_PATH, 3);
		log.info("Replication agents list: {}", agents);
		if (agents.path(PUBLISH_DIST_AGENT).isMissingNode()) {
		    log.warn("Default distribution agent {} is missing from the distribution list", PUBLISH_DIST_AGENT);
		    return false;
		}
		return true;
	}
    
    private void isPublishQueueEmptyOfPath(String replicatedPath) throws Exception {
        new Polling(() -> isQueueEmptyOfPath(PUBLISH_DIST_AGENT, replicatedPath))
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
    private boolean isQueueEmptyOfPath(String agentName, String replicatedPath) throws Exception {
        JsonNode queuesJson = rClient.doGetJson(QUEUES_PATH, 2, 200, 300).get("queues");
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
