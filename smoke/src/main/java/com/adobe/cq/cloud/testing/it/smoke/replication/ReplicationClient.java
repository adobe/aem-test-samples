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

package com.adobe.cq.cloud.testing.it.smoke.replication;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException;
import com.adobe.cq.cloud.testing.it.smoke.exception.SmokeTestException;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agent;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agents;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Queue;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.ReplicationResponse;
import com.adobe.cq.cloud.testing.it.smoke.rules.ContentPublishRule;
import com.adobe.cq.testing.client.CQClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException.ACTIVATION_REQUEST_FAILED;
import static com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException.DEACTIVATION_REQUEST_FAILED;
import static com.adobe.cq.cloud.testing.it.smoke.exception.SmokeTestException.GENERIC;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Extension of CQClient to add replication methods
 */
public class ReplicationClient extends CQClient {
    private static final Logger log = LoggerFactory.getLogger(ContentPublishRule.class);

    private static final String BLOCKED = "BLOCKED";

    // uses "NOSONAR" because CQRules:CQBP-71 is triggering, but can be ignored for this test case
    protected static final String DIST_AGENTS_PATH = "/libs/sling/distribution/services/agents"; //NOSONAR

    @SuppressWarnings("unused")
    public ReplicationClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    @SuppressWarnings("unused")
    public ReplicationClient(URI serverUrl, String user, String password) throws ClientException {
        super(serverUrl, user, password);
    }

    /**
     * Activates the given path on author
     *
     * @param nodePath the path to activate
     * @return a replication response containing status and message
     * @throws SmokeTestException exception containing details
     */
    public ReplicationResponse activate(String agent, String nodePath) throws SmokeTestException {
        try {
            log.info("Activating {} on {}", nodePath, agent);
            ReplicationResponse response = ReplicationResponse.from(activateInternal("Activate", agent, nodePath));
            if (response.getCode() != HttpStatus.SC_OK) {
                throw getReplicationException(ACTIVATION_REQUEST_FAILED, response.getMessage(), null);
            }
            log.info("Activation response received {}", response);
            return response;
        } catch (ClientException | RuntimeException e) {
            throw getGenericException("Exception during activation", e);
        }
    }

    /**
     * Deactivates a given path on author
     *
     * @param pagePath the path to deactivate
     * @return a replication response containing status and message
     * @throws SmokeTestException exception containing error details if any
     */
    public ReplicationResponse deactivate(String agent, String pagePath) throws SmokeTestException {
        try {
            log.info("De-Activating {} on {}", pagePath, agent);
            ReplicationResponse response = ReplicationResponse.from(activateInternal("Deactivate", agent, pagePath));
            if (response.getCode() != HttpStatus.SC_OK) {
                throw getReplicationException(DEACTIVATION_REQUEST_FAILED, response.getMessage(), null);
            }
            log.info("De-Activation response received {}", response);
            return response;
        } catch (ClientException | RuntimeException e) {
            throw getGenericException("Exception during deactivation", e);
        }
    }

    private SlingHttpResponse activateInternal(String cmd, String agent, String nodePath) throws ClientException {
        FormEntityBuilder formEntityBuilder =
            FormEntityBuilder.create().addParameter("cmd", cmd).addParameter("_charset_", "utf-8").addParameter("path", nodePath).addParameter("sync", String.valueOf(true));
        if (StringUtils.isNotBlank(agent)) {
            formEntityBuilder.addParameter("agentId", agent);
        }

        return this.doPost("/bin/replicate.json", formEntityBuilder.build(), Collections.emptyList());
    }

    /**
     * Checks if the given package with paths and id still in queue
     * 
     * @param agent the json representation retrieved
     * @param replicatedPath path to check
     * @param id id to check
     * @return true if package still in queue
     */
    public static boolean checkPackageInQueue(Agent agent, String replicatedPath, String id) {
        // Filter non empty queues
        List<Queue> nonEmptyQueues = agent.getQueues().values().stream().filter(queue -> !queue.isEmpty()).collect(Collectors.toList());
        
        // Filter package details containing path or pkgId
        return nonEmptyQueues.stream().anyMatch(queue -> queue.getPackageMap().values().stream().anyMatch(pkg -> {
            boolean isIdSame = true;
            if (isNotEmpty(id) && isNotEmpty(pkg.getPkgId())) {
                isIdSame = pkg.getPkgId().equals(id);
            }
            boolean containsPkg = pkg.getPaths().contains(replicatedPath) && isIdSame;
            if (containsPkg) {
                log.warn("The replication queue {} contains item [id: {}, pkgId: {}] with paths {}",
                    agent.getName(), pkg.getId(), pkg.getPkgId(), pkg.getPaths());
            } else {
                if (pkg.isBlocked()) {
                    log.warn(
                        "The replication queue {} blocked with item [id: {}, pkgId: {}] having paths {} "
                            + "with " + "error {}", agent.getName(), pkg.getId(), pkg.getPkgId(),
                        pkg.getPaths(), pkg.getErrorMessage());
                }
            }
            return containsPkg;
        }));
    }

    /**
     * Checks if agent exists
     * 
     * @param agents the json representation retrieved
     * @param agentName the agent
     * @return true if agent exists
     */
    public static boolean checkDistributionAgentExists(Agents agents, String agentName) {
        boolean agentPresent = agents.isAgentPresent(agentName);
        if (!agentPresent) {
            log.warn("Distribution agent {} is missing from the distribution list", agentName);
            return false;
        }
        return true;
    }

    /**
     * Checks if the given agent queue is blocked
     * 
     * @param agents the json representation retrieved
     * @param agentName the agent
     * @return true if agent blocked
     */
    public static boolean isAgentQueueBlocked(Agents agents, String agentName) {
        Optional<Agent> agent = agents.getAgentBlocked(agentName);
        return agent.isPresent();
    }

    /**
     * Retrieve the agents from the author
     * 
     * @return Agents object
     * @throws SmokeTestException if any error
     */
    public Agents getAgentQueueJson() throws SmokeTestException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            SlingHttpResponse response = this.doGet(DIST_AGENTS_PATH + ".3.json", HttpUtils.getExpectedStatus(200));
            return mapper.readValue(response.getContent(), Agents.class);
        } catch (IOException | ClientException e) {
            throw new SmokeTestException(GENERIC, "Exception getting agent queues", e);
        }
    }

    public List<String> getBlockedQueueNames(Agent agent) throws SmokeTestException {
        List<String> blockedQueues = new ArrayList<>();
        try {
            SlingHttpResponse response = this.doGet(DIST_AGENTS_PATH + "/" + agent.getName() + "/queues.1.json", HttpUtils.getExpectedStatus(200));
            JsonElement jsonElement = JsonParser.parseString(response.getContent().trim());
            JsonObject result = jsonElement.getAsJsonObject();
            JsonArray items = result.getAsJsonArray("items");
            for (int i = 0; i < items.size(); i++) {
                JsonElement item = items.get(i);
                String queueName = item.getAsString();
                JsonObject queue = result.getAsJsonObject(queueName);
                if (queue.get("state").getAsString().equalsIgnoreCase(BLOCKED)) {
                    blockedQueues.add(queueName);
                }
            }
        } catch(ClientException e) {
            throw new SmokeTestException(GENERIC, "Exception getting blocked queues names", e);
        } finally {
            return blockedQueues;
        }
    }

    public void clearQueue(Agent agent) throws SmokeTestException {
        List<String> blockedQueues = this.getBlockedQueueNames(agent);
        for (String queueName: blockedQueues) {
            log.info("Clearing blocked queue {} for agent {}", queueName, agent.getName());
            try {
                FormEntityBuilder formEntityBuilder = FormEntityBuilder.create().addParameter("operation", "delete").addParameter("limit", "-1");
                this.doPost(DIST_AGENTS_PATH + "/" + agent.getName() + "/queues/" + queueName, formEntityBuilder.build(), Collections.emptyList());
            } catch(ClientException e) {
                throw new SmokeTestException(GENERIC, "Exception clearing the blocked queues", e);
            }
        }
    }

    public ReplicationException getReplicationException(String code, String message, Throwable t) {
        ReplicationException exception = new ReplicationException(code, message, t);
        log.error(exception.getMessage(), t);
        return exception;
    }

    public SmokeTestException getGenericException(String message, Throwable t) {
        SmokeTestException exception = new SmokeTestException(GENERIC, message, t);
        log.error(exception.getMessage(), t);
        return exception;
    }
}