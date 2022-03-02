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

package com.adobe.cq.cloud.testing.it.smoke.rules;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.adobe.cq.cloud.testing.it.smoke.exception.PublishException;
import com.adobe.cq.cloud.testing.it.smoke.exception.SmokeTestException;
import com.adobe.cq.cloud.testing.it.smoke.replication.ReplicationClient;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agents;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.ReplicationResponse;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.AssumptionViolatedException;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.cq.cloud.testing.it.smoke.exception.PublishException.getPageErrorCode;
import static com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException.ITEM_NOT_REPLICATED;
import static com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException.QUEUE_BLOCKED;
import static com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException.REPLICATION_UNAVAILABLE;
import static com.adobe.cq.cloud.testing.it.smoke.replication.ReplicationClient.checkPackageInQueue;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_MOVED_PERMANENTLY;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS;

/**
 * Junit test rule to check content distribution functionality
 */
public class ContentPublishRule extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(ContentPublishRule.class);
    
    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    protected static final long TIMEOUT_PER_TRY = TimeUnit.MINUTES.toMillis(1);

    protected static final String PUBLISH_DIST_AGENT = "publish";
    
    private final Page root;

    private final Instance authorRule;
    private final Instance publishRule;

    private ReplicationClient replicationClient;

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
     * Assert publish agent queues not blocked
     * 
     * @throws Exception if exception occurs
     */
    @Override
    protected void before() throws Exception {
        replicationClient = getAuthorClient().adaptTo(ReplicationClient.class);
        doReplicationChecks();
    }

    /**
     * The client to use for page operations. The default implementation creates a {@link CQClient}.
     * The default implementation also uses the default admin user.
     *
     * @return The client to use for page operations.
     */
    protected CQClient getPublishClient() {
        if (publishClient == null) {
            publishClient = publishRule.getAdminClient(CQClient.class);
        }
        return publishClient;
    }

    /**
     * The author client to use for page operations. The default implementation creates a {@link CQClient}.
     * The default implementation also uses the default admin user.
     *
     * @return The client to use for page operations.
     */
    protected CQClient getAuthorClient() {
        if (authorClient == null) {
            authorClient =  authorRule.getAdminClient(CQClient.class);
        }
        return authorClient;
    }

    private void checkPage(final int expectedStatus) throws Exception {
        checkPage(true, expectedStatus);
    }
    
    /**
     * Checks that a GET on the page on publish has the {{expectedStatus}} in the response
     *
     * @throws Exception if an error occurred
     * @return
     */
    private void checkPage(boolean skipDispatcherCache, final int expectedStatus) throws PublishException {
        final String path = root.getPath() + ".html";
        log.info("Checking page {} returns status {}", getPublishClient().getUrl(path), expectedStatus);
        String errorMessage = String.format("Failed to check the page %s via the AEM publish ingress (expected status %s). "
            + "Please ensure that the CDN and Dispatcher configurations allow fetching the page.", path, expectedStatus);

        try {
            SlingHttpResponse res = null;
            final List<NameValuePair> queryParams = skipDispatcherCache
                ? Collections.singletonList(
                new BasicNameValuePair("timestamp", String.valueOf(System.currentTimeMillis())))
                : Collections.emptyList();
            
            URI uri = getPublishClient().getUrl(path, queryParams);
            HttpUriRequest request = new HttpGet(uri);
            // Disable following redirects
            request.setParams(new BasicHttpParams().setParameter(HANDLE_REDIRECTS, false));
            
            res  = getPublishClient().doStreamRequest(request, null);
            
            // Special handling for 401,403, logging for 301,302
            if (null != res && (res.getStatusLine().getStatusCode() == SC_UNAUTHORIZED
                || res.getStatusLine().getStatusCode() == SC_FORBIDDEN)) {
                log.warn("Got status {} while checking page, expected status {}", res.getStatusLine().getStatusCode(),
                    expectedStatus);
                throw new AssumptionViolatedException("Publish requires auth for (SAML?) or not authorized. Skipping...");
            } else if (null != res && (res.getStatusLine().getStatusCode() == SC_MOVED_PERMANENTLY 
                || res.getStatusLine().getStatusCode() == SC_MOVED_TEMPORARILY)) {
                log.info("Redirect status {} detected for page {}", res.getStatusLine().getStatusCode(), path);
            } else if (null != res && (res.getStatusLine().getStatusCode() == expectedStatus)) {
                log.info("Page check completed with status {}", res.getStatusLine().getStatusCode());
                return;
            }
            
            // Continue with a retry if any other status
            Polling polling = null;
            try {
                polling = new Polling(() -> {
                    final List<NameValuePair> newQueryParams = skipDispatcherCache
                        ? Collections.singletonList(
                        new BasicNameValuePair("timestamp", String.valueOf(System.currentTimeMillis())))
                        : Collections.emptyList();
                    SlingHttpResponse slingHttpResponse =
                        getPublishClient().doGet(path, newQueryParams, Collections.emptyList(), expectedStatus);
                    log.info("Page check completed with status {}", slingHttpResponse.getStatusLine().getStatusCode());
                    return true;
                });
                polling.poll(TIMEOUT_PER_TRY, 1000);
            } catch (TimeoutException te) {
                throw getPublishException(getPageErrorCode(expectedStatus), errorMessage, polling.getLastException());
            } catch (InterruptedException e) {
                throw getPublishException(getPageErrorCode(expectedStatus), errorMessage, e);
            }
        } catch (ClientException e) {
            throw getPublishException(getPageErrorCode(expectedStatus), errorMessage, e);
        }
    }

    private PublishException getPublishException(String code, String message, Throwable t) {
        PublishException exception = new PublishException(code, message, t);
        log.error(exception.getMessage(), exception);
        return exception;
    }
    
    public void activateAssertPublish() throws Exception {
        // Activate Page
        ReplicationResponse replicationResponse = replicationClient.activate(root.getPath());

        // Check activation successful
        waitPublishQueueEmptyOfPath(replicationResponse.getId(), root.getPath(), "Activate");
        
        // Assert page added on publish
        checkPage(SC_OK);
    }

    public void deactivateAssertPublish() throws Exception {
        // Deactivate Page
        ReplicationResponse replicationResponse = replicationClient.deactivate(root.getPath());
        
        // Check deactivation successful
        waitPublishQueueEmptyOfPath(replicationResponse.getId(), root.getPath(), "Deactivate");
        
        // Assert page deleted on publish
        checkPage(SC_NOT_FOUND);
    }
    
    private void waitPublishQueueEmptyOfPath(String id, String path, String action)
        throws SmokeTestException {
       waitQueueEmptyOfPath(PUBLISH_DIST_AGENT, path, id, action);
    }

    /**
     * Checks presence of publish distribution agent and waits until timeout if it's available
     * Checks if the publish agent is not blocked
     * 
     * @throws SmokeTestException exception if problems
     */
    private void doReplicationChecks() throws SmokeTestException {
        Polling polling = null;
        AtomicReference<Agents> agentsRef = new AtomicReference<>();

        log.info("Checking Replication agents available and not blocked");

        // Check if the publish agent is present and retry till timeout
        try {
            polling = new Polling(() -> {
                agentsRef.set(replicationClient.getAgentQueueJson());
                log.info("Replication agents list: {}", agentsRef.get());

                return ReplicationClient.checkDistributionAgentExists(agentsRef.get(), PUBLISH_DIST_AGENT);
            });
            polling.poll(TIMEOUT, 500);
        } catch (TimeoutException e) {
            throw replicationClient.getReplicationException(REPLICATION_UNAVAILABLE, 
                String.format("Replication agent %s unavailable", PUBLISH_DIST_AGENT), polling.getLastException());
        } catch (Exception e) {
            throw replicationClient.getGenericException("Replication agent unavailable", e);
        }

        Agents agents = agentsRef.get();
        
        // throw if publish agent is blocked
        boolean agentQueueBlocked = ReplicationClient.isAgentQueueBlocked(agents, PUBLISH_DIST_AGENT);
        if (agentQueueBlocked) {
            throw replicationClient.getReplicationException(QUEUE_BLOCKED, 
                "Replication agent queue blocked - " + agents.getAgent(PUBLISH_DIST_AGENT), null);
        }
    }
    
    /**
     * Checks if the agent queue contains the given id and the path until timeout.
     *
     * @param agent   queue to check
     * @param path    path being replicated
     * @param id      identifier for the replication request
     * @param action  the action initiated Activate or Deactivate
     * @throws SmokeTestException exception containing error details if any
     */
    public void waitQueueEmptyOfPath(final String agent, final String path, final String id, final String action)
        throws SmokeTestException {
        Polling polling = null;
        AtomicReference<Agents> agentsRef = new AtomicReference<>();
        
        log.info("Checking the replication queue [{}] for action [{}] contains item [pkgId: {}] with paths [{}]",
            agent, action, id, path);
        
        // Check if the agent has the package
        try {
            polling = new Polling(() -> {
                agentsRef.set(replicationClient.getAgentQueueJson());
                return !checkPackageInQueue(agentsRef.get().getAgent(agent), path, id);
            });
            polling.poll(TIMEOUT, 2000);
        } catch (TimeoutException e) {
            log.info("Agent not empty of item {}", ((agentsRef.get() != null) ? agentsRef.get().getAgent(agent) : ""));
            throw replicationClient.getReplicationException(ITEM_NOT_REPLICATED, 
                String.format("Item not activated within %s ms", TIMEOUT),
                polling.getLastException());
        } catch (Exception e) {
            throw replicationClient.getGenericException(String.format("Item not activated within %s ms", TIMEOUT), e);
        }
    }
}

