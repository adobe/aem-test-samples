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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import static com.adobe.cq.cloud.testing.it.smoke.replication.data.Agents.AgentsDeserializer;

@JsonDeserialize(using = AgentsDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Agents {
    @JsonProperty()
    private Map<String, Agent> agents = new HashMap<>();

    private void addAgent(String id, Agent agent) {
        agents.put(id, agent);
    }

    public Agent getAgent(String id) {
        return agents.get(id);
    }
    
    public boolean isAgentPresent(String agent) {
        return agents.containsKey(agent);
    }
    
    public Optional<Agent> getAgentBlocked(String agentName) {
        return agents.values().stream().filter(
            agent -> agent.isBlocked() && agent.getName().equalsIgnoreCase(agentName)).findAny();
    }
    
    @Override 
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writeValueAsString(agents);
        } catch (JsonProcessingException ignored) {}
        return "";
    }

    public static class AgentsDeserializer extends StdDeserializer<Agents> {

        @SuppressWarnings("unused")
        protected AgentsDeserializer() {
            this(null);
        }
        
        protected AgentsDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Agents deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
            Agents agents = new Agents();
            
            JsonNode agentsNode = jp.getCodec().readTree(jp);   
            if (agentsNode != null && !agentsNode.isMissingNode()) {
                Iterator<JsonNode> itemsIterator = agentsNode.path("items").elements();

                while (itemsIterator.hasNext()) {
                    String agent = itemsIterator.next().asText();
                    agents.addAgent(agent, Agent.fromJson(agentsNode.path(agent)));
                }
            }
            
            return agents;
        }
    }
}
