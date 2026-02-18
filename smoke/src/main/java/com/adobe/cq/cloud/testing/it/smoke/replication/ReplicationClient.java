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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.adobe.cq.cloud.testing.it.smoke.exception.ReplicationException;
import com.adobe.cq.cloud.testing.it.smoke.exception.SmokeTestException;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agent;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agents;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Queue;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.ReplicationResponse;
import com.adobe.cq.cloud.testing.it.smoke.rules.ContentPublishRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.adobe.cq.testing.client.CQClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
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
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int JSON_MAX_CHOICE_FOLLOW = 10;

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
            if (!response.getContentType().equals(CONTENT_TYPE_JSON)) {
                String msg = String.format("for the call to /bin/replicate.json received incorrect content-type '%s' instead of '%s'",
                        response.getContentType(), CONTENT_TYPE_JSON);
                throw new SmokeTestException(ACTIVATION_REQUEST_FAILED,msg, null);
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

    /**
     * Checks whether a distribution agent exists without fetching the full agents listing.
     *
     * <p>This avoids large /infinity JSON responses which can yield HTTP 300 with split resources.</p>
     *
     * @param agentName agent name (e.g. publish-internal, publish)
     * @return true if agent exists, false if not found
     * @throws SmokeTestException if a connection or unexpected status occurs
     */
    public boolean distributionAgentExists(String agentName) throws SmokeTestException {
        String path = DIST_AGENTS_PATH + "/" + agentName + ".0.json";
        try {
            SlingHttpResponse response = doGetAllowingAnyStatus(path);
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                return true;
            }
            if (status == HttpStatus.SC_NOT_FOUND) {
                return false;
            }
            // In case the JSON servlet decides to split, treat as "exists".
            if (status == HttpStatus.SC_MULTIPLE_CHOICES) {
                log.warn("Got 300 while checking agent existence at {}", path);
                return true;
            }
            throw new SmokeTestException(GENERIC,
                String.format("Unexpected HTTP status %s while checking agent existence at %s", status, path), null);
        } catch (ClientException e) {
            throw new SmokeTestException(GENERIC, "Exception checking distribution agent existence", e);
        }
    }

    /**
     * Fetches a single distribution agent JSON and converts it to {@link Agent}.
     *
     * <p>This is intentionally per-agent to keep responses small and stable.</p>
     *
     * @param agentName agent name (e.g. publish-internal, publish)
     * @return parsed agent
     * @throws SmokeTestException if a connection or parsing error occurs
     */
    public Agent getAgent(String agentName) throws SmokeTestException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Never use .3.json: build the agent view from small, stable endpoints.
            JsonNode agentNode =
                readJsonFollowingMultipleChoices(mapper, DIST_AGENTS_PATH + "/" + agentName + ".0.json");
            JsonNode queuesNode =
                readJsonFollowingMultipleChoices(mapper, DIST_AGENTS_PATH + "/" + agentName + "/queues.1.json");

            Agent agent = new Agent();
            agent.setName(agentNode.path("name").asText(agentName));
            agent.setState(agentNode.path("status").path("state").asText(""));

            Map<String, Queue> queues = new HashMap<>();
            for (JsonNode queueNameNode : queuesNode.path("items")) {
                String queueName = queueNameNode.asText();
                if (StringUtils.isBlank(queueName)) {
                    continue;
                }

                JsonNode queueNode = queuesNode.path(queueName);
                // If the queue wasn't expanded in the queues listing, fetch it directly.
                if (queueNode.isMissingNode() || queueNode.isNull()) {
                    queueNode = readJsonFollowingMultipleChoices(mapper,
                        DIST_AGENTS_PATH + "/" + agentName + "/queues/" + queueName + ".1.json");
                }

                Queue queue = Queue.fromJson(queueNode);
                queue.setName(queueName);
                queues.put(queueName, queue);
            }
            agent.setQueues(queues);
            return agent;
        } catch (IOException | ClientException e) {
            throw new SmokeTestException(GENERIC, "Exception fetching distribution agent details", e);
        }
    }

    private SlingHttpResponse doGetAllowingAnyStatus(String path) throws ClientException {
        HttpUriRequest request = new HttpGet(getUrl(path));
        return this.doStreamRequest(request, null);
    }

    private JsonNode readJsonFollowingMultipleChoices(ObjectMapper mapper, String path)
        throws ClientException, IOException, SmokeTestException {
        SlingHttpResponse response = doGetAllowingAnyStatus(path);
        int status = response.getStatusLine().getStatusCode();
        if (status == HttpStatus.SC_OK) {
            return mapper.readTree(response.getContent());
        }
        if (status == HttpStatus.SC_MULTIPLE_CHOICES) {
            List<String> choices = parseMultipleChoicePaths(response.getContent());
            int attempts = 0;
            for (String choice : choices) {
                if (attempts++ >= JSON_MAX_CHOICE_FOLLOW) {
                    break;
                }
                SlingHttpResponse choiceResponse = doGetAllowingAnyStatus(choice);
                if (choiceResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return mapper.readTree(choiceResponse.getContent());
                }
            }
            throw new SmokeTestException(GENERIC,
                String.format("Could not resolve 300 Multiple Choices for %s", path), null);
        }
        throw new SmokeTestException(GENERIC,
            String.format("Unexpected HTTP status %s for %s", status, path), null);
    }

    private List<String> parseMultipleChoicePaths(String responseBody) {
        try {
            JsonElement jsonElement = JsonParser.parseString(responseBody.trim());
            if (!jsonElement.isJsonArray()) {
                return Collections.emptyList();
            }
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            List<String> choices = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                if (element != null && element.isJsonPrimitive()) {
                    String choice = element.getAsString();
                    if (StringUtils.isNotBlank(choice)) {
                        choices.add(choice);
                    }
                }
            }
            return choices;
        } catch (RuntimeException e) {
            log.warn("Failed to parse 300 Multiple Choices response: {}", responseBody, e);
            return Collections.emptyList();
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
        } catch (ClientException e) {
            throw new SmokeTestException(GENERIC, "Exception getting blocked queues names", e);
        }
        return blockedQueues;
    }

    public void clearQueue(Agent agent) throws SmokeTestException {
        List<String> blockedQueues = this.getBlockedQueueNames(agent);
        for (String queueName: blockedQueues) {
            log.info("Clearing blocked queue {} for agent {}", queueName, agent.getName());
            try {
                FormEntityBuilder formEntityBuilder = FormEntityBuilder.create().addParameter("operation", "delete").addParameter("limit", "-1");
                this.doPost(DIST_AGENTS_PATH + "/" + agent.getName() + "/queues/" + queueName, formEntityBuilder.build(), Collections.emptyList());
            } catch (ClientException e) {
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