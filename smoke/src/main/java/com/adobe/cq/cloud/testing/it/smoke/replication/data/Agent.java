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

package com.adobe.cq.cloud.testing.it.smoke.replication.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Agent {

    private static final String BLOCKED = "BLOCKED";
    
    @JsonProperty("state")
    private String state;

    @JsonProperty("name")
    private String name;

    @JsonProperty("queues")
    private Map<String, Queue> queues = new HashMap<>();

    private void addQueue(String id, Queue queue) {
        queues.put(id, queue);
    }

    public Map<String, Queue> getQueues() {
        return queues;
    }
    
    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @JsonIgnore
    public boolean isBlocked() {
        return getState().equalsIgnoreCase(BLOCKED);
    }

    @Override 
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException ignored) {}
        return "";
    }

    public static Agent fromJson(JsonNode node) {
        Agent agent = new Agent();
        
        if (node != null && !node.isMissingNode()) {
            agent.setState(node.path("status").path("state").asText());
            agent.setName(node.path("name").asText());
            
            JsonNode queuesJson = node.path("queues");
            Iterator<JsonNode> itemsIterator = queuesJson.path("items").elements();

            while (itemsIterator.hasNext()) {
                String queue = itemsIterator.next().asText();
                agent.addQueue(queue, Queue.fromJson(queuesJson.path(queue)));
            }
        }
        return agent;
    }
}