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

import java.io.IOException;

import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agent;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.Agents;
import com.adobe.cq.cloud.testing.it.smoke.replication.data.ReplicationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;

import static com.adobe.cq.cloud.testing.it.smoke.replication.ReplicationClient.checkDistributionAgentExists;
import static com.adobe.cq.cloud.testing.it.smoke.replication.ReplicationClient.checkPackageInQueue;
import static com.adobe.cq.cloud.testing.it.smoke.replication.ReplicationClient.isAgentQueueBlocked;
import static com.adobe.cq.cloud.testing.it.smoke.replication.data.ReplicationResponse.parseJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the various data objects for json parsed methods
 */
public class AgentsTest {
    
    @Test
    public void runningIncompleteQueueState() throws IOException {
        String json =
            "{\n" + "  \"sling:resourceType\": \"sling/distribution/service/agent/list\",\n" + "  \"items\": [\n" + 
                "    \"preview\",\n" + "    \"publish\"\n" + "  ],\n" + "  \"preview\": {\n"
                + "    \"name\": \"preview\",\n" + "    \"sling:resourceType\": \"sling/distribution/service/agent\",\n"
                + "    \"queues\": {\n" + "      \"sling:resourceType\": \"sling/distribution/service/agent/queue"
                + "/list\",\n" + "      \"items\": [\n" + "        \"6f079cc9-18fe-4168-b6dc-121436c356ee-previewSubscriber\",\n"
                + "        \"770c893c-60ee-47de-a9b0-ad985addbfea-previewSubscriber\"\n" + "      ],\n"
                + "      \"6f079cc9-18fe-4168-b6dc-121436c356ee-previewSubscriber\": {\n" + "        \"capabilities" 
                + "\": [],\n"
                + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n" + "        \"state" 
                + "\": \"IDLE\",\n" + "        \"items\": [],\n" + "        \"itemsCount\": 0,\n"
                + "        \"empty\": true\n" + "      },\n" + "      \"770c893c-60ee-47de-a9b0-ad985addbfea-previewSubscriber\": {\n"
                + "        \"capabilities\": [\n" + "          \"removable\",\n" + "          \"clearable\"\n" + "   "
                + "     ],\n" + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n" + 
                "        \"state\": \"IDLE\",\n" + "        \"items\": [],\n" + "        \"itemsCount\": 0,\n"
                + "        \"empty\": true\n" + "      }\n" + "    },\n" + "    \"log\": {\n" + "      \"sling"
                + ":resourceType\": \"sling/distribution/service/log\"\n" + "    },\n" + "    \"status\": {\n" + 
                "      \"state\": \"IDLE\"\n" + "    }\n" + "  },\n" + "  \"publish\": {\n"
                + "    \"name\": \"publish\",\n" + "    \"sling:resourceType\": \"sling/distribution/service/agent\",\n"
                + "    \"queues\": {\n"
                + "      \"sling:resourceType\": \"sling/distribution/service/agent/queue/list\",\n"
                + "      \"items\": [\n" + "        \"3b091547-e734-4a0d-9558-9fb198a52a6b-publishSubscriber\",\n"
                + "        \"27756db5-7160-41d7-bb46-595f82ad0558-publishSubscriber\",\n"
                + "        \"efb9696b-135a-4fe4-81fb-37d867a77826-publishSubscriber\"\n" + "      ],\n"
                + "      \"3b091547-e734-4a0d-9558-9fb198a52a6b-publishSubscriber\": {\n"
                + "        \"capabilities\": [],\n" + "        \"sling:resourceType\": \"sling/distribution/service"
                + "/agent/queue\",\n" + "        \"state\": \"RUNNING\",\n" + "        \"items\": [\n" + 
                "          \"package-0@1918741\"\n"
                + "        ],\n" + "        \"itemsCount\": 3,\n" + "        \"empty\": false\n" + "      },\n"
                + "      \"27756db5-7160-41d7-bb46-595f82ad0558-publishSubscriber\": {\n" + "        \"capabilities"
                + "\": [\n" + "          \"removable\",\n" + "          \"clearable\"\n" + "        ],\n"
                + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n"
                + "        \"state\": \"RUNNING\",\n" + "        \"items\": [\n" + "          \"package-0@1918741\"\n" + "        ],\n" + "        \"itemsCount\": 3,\n" + "        \"empty\": false\n" + "      },\n"
                + "      \"efb9696b-135a-4fe4-81fb-37d867a77826-publishSubscriber\": {\n" + 
                "        \"capabilities\": [],\n"
                + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n" + "        \"state"
                + "\": \"RUNNING\",\n" + "        \"items\": [\n" + "          \"package-0@1918741\"\n" + "        ],\n"
                + "        \"itemsCount\": 3,\n" + "        \"empty\": false\n" + "      }\n" + "   " + " },\n"
                + "    \"log\": {\n" + "      \"sling:resourceType\": \"sling/distribution/service/log\"\n" + "    },\n"
                + "    \"status\": {\n" + "      \"state\": \"RUNNING\"\n" + "    }\n" + "  }\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Agents agents = mapper.readValue(json, Agents.class);
        assertTrue(checkDistributionAgentExists(agents,"publish"));
        assertTrue(checkDistributionAgentExists(agents, "preview"));
        assertFalse(isAgentQueueBlocked(agents, "publish"));

        Agent publish = agents.getAgent("publish");
        assertNotNull(publish);

        assertFalse(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
        assertFalse(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            ""));
        assertFalse(checkPackageInQueue(publish, "",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
    }
    
    @Test
    public void blockedRunningQueueState() throws IOException {
        String json =
            "{\n" + "  \"sling:resourceType\": \"sling/distribution/service/agent/list\",\n" + "  \"items\": [\n" + 
                "    \"publish\"\n" + "  ],\n" + "  \"publish\": {\n"
                + "    \"name\": \"publish\",\n" + "    \"sling:resourceType\": \"sling/distribution/service/agent\",\n"
                + "    \"queues\": {\n" + 
                "      \"sling:resourceType\": \"sling/distribution/service/agent/queue/list\",\n"
                + "      \"items\": [\n" + "        \"31671be6-39fe-4841-8bfc-0246b44b6930-publishSubscriber\",\n" + "        \"546aa35f-1eb0-462d-aacf-ea227d35d28e-publishSubscriber\"\n" + "      ],\n"
                + "      \"31671be6-39fe-4841-8bfc-0246b44b6930-publishSubscriber\": {\n" + "        \"capabilities\": [\n"
                + "          \"removable\",\n" + "          \"clearable\"\n" + "        ],\n" + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n"
                + "        \"state\": \"RUNNING\",\n" + "        \"items\": [\n" + "          \"package-0@58870963\"\n"
                + "        ],\n" + "        \"itemsCount\": 1,\n" + "        \"empty\": false,\n" + 
                "        \"package-0@58870963\": {\n" + "          \"size\": 7794,\n" + "          \"paths\": [\n"
                + "            \"/content/dam/test/atari65xe_desc.html\"\n" + "          ],\n" + "          \"sling" 
                + ":resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
                + "          \"action\": \"ADD\",\n" + "          \"id\": \"package-0@58870963\",\n" + "          " 
                + "\"pkgId\": \"dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6\",\n"
                + "          \"time\": \"Fri Feb 18 09:13:56 UTC 2022\",\n" + "          \"state\": \"QUEUED\",\n" + 
                "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        }\n"
                + "      },\n" + "      \"546aa35f-1eb0-462d-aacf-ea227d35d28e-publishSubscriber\": {\n" + "        " 
                + "\"capabilities\": [],\n"
                + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n" + "        \"state\": \"BLOCKED\",\n" + "        \"items\": [\n" + "          \"package-0@58870963\"\n"
                + "        ],\n" + "        \"itemsCount\": 1,\n" + "        \"empty\": false,\n" + "        " 
                + "\"package-0@58870963\": {\n" + "          \"size\": 7794,\n" + "          \"paths\": [\n"
                + "            \"/content/dam/test/atari65xe8_desc.html\"\n" + "          ],\n" + "          \"errorMessage\": \"Failed attempt (12/infinite) to import the distribution package\",\n"
                + "          \"sling:resourceType\": \"sling/distribution/service/agent/queue/item\",\n" + 
                "          \"action\": \"ADD\",\n" + "          \"id\": \"package-0@58870963\",\n"
                + "          \"pkgId\": \"dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e8\",\n" + 
                "          \"time\": \"Fri Feb 18 09:13:56 UTC 2022\",\n" + "          \"state\": \"ERROR\",\n"
                + "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        }\n" + "      }\n" + "    },\n" + "    \"log\": {\n"
                + "      \"sling:resourceType\": \"sling/distribution/service/log\"\n" + "    },\n" + "    \"status\": {\n"
                + "      \"state\": \"BLOCKED\"\n" + "    }\n" + "  }\n" + "}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Agents agents = mapper.readValue(json, Agents.class);

        assertTrue(checkDistributionAgentExists(agents,"publish"));
        assertFalse(checkDistributionAgentExists(agents, "preview"));
        assertTrue(isAgentQueueBlocked(agents, "publish"));

        Agent publish = agents.getAgent("publish");
        assertNotNull(publish);
        
        assertTrue(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html", 
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
        assertTrue(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            ""));
    }
    
    @Test
    public void runningQueueState() throws IOException {
        String json =
            "{\n" + "  \"sling:resourceType\": \"sling/distribution/service/agent/list\",\n" + "  \"items\": [\n" + 
                "    \"publish\"\n" + "  ],\n" + "  \"publish\": {\n"
                + "    \"name\": \"publish\",\n" + "    \"sling:resourceType\": \"sling/distribution/service/agent\",\n"
                + "    \"queues\": {\n" + 
                "      \"sling:resourceType\": \"sling/distribution/service/agent/queue/list\",\n"
                + "      \"items\": [\n" + "        \"31671be6-39fe-4841-8bfc-0246b44b6930-publishSubscriber\",\n" + "        \"546aa35f-1eb0-462d-aacf-ea227d35d28e-publishSubscriber\"\n" + "      ],\n"
                + "      \"31671be6-39fe-4841-8bfc-0246b44b6930-publishSubscriber\": {\n" + "        \"capabilities\": [\n"
                + "          \"removable\",\n" + "          \"clearable\"\n" + "        ],\n" + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n"
                + "        \"state\": \"RUNNING\",\n" + "        \"items\": [\n" + "          \"package-0@58870963\"\n"
                + "        ],\n" + "        \"itemsCount\": 1,\n" + "        \"empty\": false,\n" + 
                "        \"package-0@58870963\": {\n" + "          \"size\": 7794,\n" + "          \"paths\": [\n"
                + "            \"/content/dam/test/atari65xe_desc.html\"\n" + "          ],\n" + "          \"sling" 
                + ":resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
                + "          \"action\": \"ADD\",\n" + "          \"id\": \"package-0@58870963\",\n" + "          " 
                + "\"pkgId\": \"dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6\",\n"
                + "          \"time\": \"Fri Feb 18 09:13:56 UTC 2022\",\n" + "          \"state\": \"QUEUED\",\n" + 
                "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        }\n"
                + "      },\n" + "      \"546aa35f-1eb0-462d-aacf-ea227d35d28e-publishSubscriber\": {\n" + "        " 
                + "\"capabilities\": [],\n"
                + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n" + "        \"state\": \"RUNNING\",\n" + "        \"items\": [\n" + "          \"package-0@58870963\"\n"
                + "        ],\n" + "        \"itemsCount\": 1,\n" + "        \"empty\": false,\n" + "        " 
                + "\"package-0@58870963\": {\n" + "          \"size\": 7794,\n" + "          \"paths\": [\n"
                + "            \"/content/dam/test/atari65xe_desc.html\"\n" + "          ],\n" + "          \"sling:resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
                + "          \"action\": \"ADD\",\n" + "          \"id\": \"package-0@58870963\",\n" + 
                "          \"pkgId\": \"dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6\",\n"
                + "          \"time\": \"Fri Feb 18 09:13:56 UTC 2022\",\n" + "          \"state\": \"QUEUED\",\n" + 
                "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        }\n"
                + "      }\n" + "    },\n" + "    \"log\": {\n" + "      \"sling:resourceType\": \"sling/distribution/service/log\"\n" + "    },\n"
                + "    \"status\": {\n" + "      \"state\": \"RUNNING\"\n" + "    }\n" + "  }\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Agents agents = mapper.readValue(json, Agents.class);
        assertTrue(checkDistributionAgentExists(agents,"publish"));
        assertFalse(checkDistributionAgentExists(agents, "preview"));
        assertFalse(isAgentQueueBlocked(agents, "publish"));

        Agent publish = agents.getAgent("publish");
        assertNotNull(publish);
        
        assertTrue(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
        assertTrue(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            ""));
    }
    
    @Test
    public void blockedQueueState() throws IOException {
        String json = "{\n" + "  \"sling:resourceType\": \"sling/distribution/service/agent/list\",\n"
            + "  \"items\": [\n" + "    \"publish\"\n" + "  ],\n" + "  \"publish\": {\n"
            + "    \"name\": \"publish\",\n" + "    \"sling:resourceType\": \"sling/distribution/service/agent\",\n"
            + "    \"queues\": {\n" + "      \"sling:resourceType\": \"sling/distribution/service/agent/queue/list\",\n"
            + "      \"items\": [\n" + "        \"546aa35f-1eb0-462d-aacf-ea227d35d28e-publishSubscriber\"\n"
            + "      ],\n" + "      \"546aa35f-1eb0-462d-aacf-ea227d35d28e-publishSubscriber\": {\n"
            + "        \"capabilities\": [],\n"
            + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n"
            + "        \"state\": \"BLOCKED\",\n" + "        \"items\": [\n" + "          \"package-0@58870963\"\n"
            + "        ],\n" + "        \"itemsCount\": 1,\n" + "        \"empty\": false,\n"
            + "        \"package-0@58870963\": {\n" + "          \"size\": 7794,\n" + "          \"paths\": [\n"
            + "            \"/content/dam/test/atari65xe8_desc.html\"\n" + "          ],\n"
            + "          \"errorMessage\": \"Failed attempt (12/infinite) to import the distribution package\",\n"
            + "          \"sling:resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
            + "          \"action\": \"ADD\",\n" + "          \"id\": \"package-0@58870963\",\n"
            + "          \"pkgId\": \"dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e68\",\n"
            + "          \"time\": \"Fri Feb 18 09:13:56 UTC 2022\",\n" + "          \"state\": \"ERROR\",\n"
            + "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        }\n"
            + "      }\n" + "    },\n" + "    \"log\": {\n"
            + "      \"sling:resourceType\": \"sling/distribution/service/log\"\n" + "    },\n" + "    \"status\": {\n"
            + "      \"state\": \"BLOCKED\"\n" + "    }\n" + "  }\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Agents agents = mapper.readValue(json, Agents.class);
        assertTrue(checkDistributionAgentExists(agents,"publish"));
        assertFalse(checkDistributionAgentExists(agents, "preview"));
        assertTrue(isAgentQueueBlocked(agents, "publish"));

        Agent publish = agents.getAgent("publish");
        assertNotNull(publish);

        assertFalse(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
        assertFalse(checkPackageInQueue(publish, "/content/dam/test/atari65xe_desc.html",
            ""));
        assertFalse(checkPackageInQueue(publish, "",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
    }
    
    @Test
    public void blockedIncompleteState() throws IOException {
        String json = "{\n" + "  \"sling:resourceType\": \"sling/distribution/service/agent/list\",\n"
            + "  \"items\": [\n" + "    \"publish\"\n" + "  ],\n" + "  \"publish\": {\n"
            + "    \"name\": \"publish\",\n" + "    \"queues\": {\n" + "      \"items\": [\n"
            + "        \"7ece8e2c-d641-4d50-a0ff-6fcd3c52f7d8-publishSubscriber\",\n"
            + "        \"0c77809c-cb61-437b-981b-0424c042fd92-publishSubscriber\"\n" + "      ],\n"
            + "      \"7ece8e2c-d641-4d50-a0ff-6fcd3c52f7d8-publishSubscriber\": {\n"
            + "        \"state\": \"BLOCKED\",\n" + "        \"items\": [\n" + "          \"package-0@52734825\"\n"
            + "        ],\n" + "        \"itemsCount\": 1,\n" + "        \"empty\": false,\n"
            + "        \"package-0@52734825\": {\n" + "          \"size\": 6073,\n" + "          \"paths\": [\n"
            + "            \"/content/test-site/testpage_8e57dec8-40e8-4e42-a267-ff5142f5c472\"\n" + "          ],\n"
            + "          \"errorMessage\": \"Failed attempt (12/infinite) to import the distribution package\"\n"
            + "        }\n" + "      },\n" + "      \"0c77809c-cb61-437b-981b-0424c042fd92-publishSubscriber\": {\n"
            + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n"
            + "        \"state\": \"IDLE\",\n" + "        \"items\": [],\n" + "        \"itemsCount\": 0,\n"
            + "        \"empty\": true\n" + "      }\n" + "    },\n" + "    \"log\": {\n"
            + "      \"sling:resourceType\": \"sling/distribution/service/log\"\n" + "    },\n" + "    \"status\": {\n"
            + "      \"state\": \"BLOCKED\"\n" + "    }\n" + "  }\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Agents agents = mapper.readValue(json, Agents.class);
        assertTrue(checkDistributionAgentExists(agents,"publish"));
        assertFalse(checkDistributionAgentExists(agents, "preview"));
        assertTrue(isAgentQueueBlocked(agents, "publish"));

        Agent publish = agents.getAgent("publish");
        assertNotNull(publish);

        assertTrue(checkPackageInQueue(publish, "/content/test-site/testpage_8e57dec8-40e8-4e42-a267-ff5142f5c472",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
        assertTrue(checkPackageInQueue(publish, "/content/test-site/testpage_8e57dec8-40e8-4e42-a267-ff5142f5c472",
            ""));
        assertFalse(checkPackageInQueue(publish, "",
            "dstrpck-1645175636103-79265f3c-779c-4f97-b572-4c81161494e6"));
    }
    
    @Test
    public void multiplePkgQueueState() throws IOException {
        String json = "{\n" + "  \"sling:resourceType\": \"sling/distribution/service/agent/list\",\n"
            + "  \"items\": [\n" + "    \"publish\"\n" + "  ],\n" + "  \"publish\": {\n"
            + "    \"name\": \"publish\",\n" + "    \"sling:resourceType\": \"sling/distribution/service/agent\",\n"
            + "    \"queues\": {\n" + "      \"sling:resourceType\": \"sling/distribution/service/agent/queue/list\",\n"
            + "      \"items\": [\n" + "        \"c4503674-aae8-4c38-aecb-7658adc41b24-publishSubscriber\"\n"
            + "      ],\n" + "      \"c4503674-aae8-4c38-aecb-7658adc41b24-publishSubscriber\": {\n"
            + "        \"capabilities\": [],\n"
            + "        \"sling:resourceType\": \"sling/distribution/service/agent/queue\",\n"
            + "        \"state\": \"RUNNING\",\n" + "        \"items\": [\n" + "          \"package-0@58932264\"\n"
            + "        ],\n" + "        \"itemsCount\": 3,\n" + "        \"empty\": false,\n"
            + "        \"package-0@58932264\": {\n" + "          \"size\": 0,\n" + "          \"paths\": [\n"
            + "            \"/content/test-site/testpage_14b07d0c-555f-415a-b1ad-ac7536ea09e2\"\n" + "          ],\n"
            + "          \"sling:resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
            + "          \"action\": \"DELETE\",\n" + "          \"id\": \"package-0@58932264\",\n"
            + "          \"pkgId\": \"54e829af-c99b-4b9c-86ba-3d7f97eb4e0e\",\n"
            + "          \"time\": \"Fri Feb 25 16:16:50 UTC 2022\",\n" + "          \"state\": \"QUEUED\",\n"
            + "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        },\n"
            + "        \"package-0@58932265\": {\n" + "          \"size\": 6443,\n" + "          \"paths\": [\n"
            + "            \"/content/test-site/testpage_14b07d0c-555f-415a-b1ad-ac7536ea09e2\"\n" + "          ],\n"
            + "          \"sling:resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
            + "          \"action\": \"ADD\",\n" + "          \"id\": \"package-0@58932265\",\n"
            + "          \"pkgId\": \"dstrpck-1645805891362-264d77b5-d5cd-4138-8632-6d613fd2b3f6\",\n"
            + "          \"time\": \"Fri Feb 25 16:18:11 UTC 2022\",\n" + "          \"state\": \"QUEUED\",\n"
            + "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        },\n"
            + "        \"package-0@58932266\": {\n" + "          \"size\": 0,\n" + "          \"paths\": [\n"
            + "            \"/content/test-site/testpage_14b07d0c-555f-415a-b1ad-ac7536ea09e2\"\n" + "          ],\n"
            + "          \"sling:resourceType\": \"sling/distribution/service/agent/queue/item\",\n"
            + "          \"action\": \"DELETE\",\n" + "          \"id\": \"package-0@58932266\",\n"
            + "          \"pkgId\": \"225635ae-c74b-4eb0-ae6a-d01f68c4c960\",\n"
            + "          \"time\": \"Fri Feb 25 16:19:12 UTC 2022\",\n" + "          \"state\": \"QUEUED\",\n"
            + "          \"userid\": \"replication-service\",\n" + "          \"attempts\": 0\n" + "        }\n"
            + "      }\n" + "    },\n" + "    \"log\": {\n"
            + "      \"sling:resourceType\": \"sling/distribution/service/log\"\n" + "    },\n" + "    \"status\": {\n"
            + "      \"state\": \"RUNNING\"\n" + "    }\n" + "  }\n" + "}";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Agents agents = mapper.readValue(json, Agents.class);
        assertTrue(checkDistributionAgentExists(agents,"publish"));
        assertFalse(checkDistributionAgentExists(agents, "preview"));
        assertFalse(isAgentQueueBlocked(agents, "publish"));

        Agent publish = agents.getAgent("publish");
        assertNotNull(publish);

        assertTrue(checkPackageInQueue(publish, "/content/test-site/testpage_14b07d0c-555f-415a-b1ad-ac7536ea09e2",
            "dstrpck-1645805891362-264d77b5-d5cd-4138-8632-6d613fd2b3f6"));
        assertTrue(checkPackageInQueue(publish, "/content/test-site/testpage_14b07d0c-555f-415a-b1ad-ac7536ea09e2",
            "dstrpck-1645805891362-264d77b5-d5cd-4138-8632-6d613fd2b3f6"));
    }
    
    @Test
    public void responseTest() throws IOException {
        String json = "{\n" + "  \"path\": [\n" + "    \"/content/dam/test/atari65xe_desc.html\"\n" + "  ],\n"
            + "  \"artifactId\": [\n" + "    \"dstrpck-1645087114730-810bc013-2c41-447e-9714-f288b0e63080\"\n"
            + "  ],\n" + "  \"status.message\": \"Replication started for /content/dam/test/atari65xe_desc.html\",\n"
            + "  \"status.code\": 200\n" + "}";
        ReplicationResponse res = new ReplicationResponse();
        ObjectMapper mapper = new ObjectMapper();
        parseJson(res, mapper.readTree(json));
        assertEquals("dstrpck-1645087114730-810bc013-2c41-447e-9714-f288b0e63080", res.getId());
        assertEquals("Replication started for /content/dam/test/atari65xe_desc.html", res.getMessage());
    }
}
