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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;

import static org.apache.sling.testing.clients.util.JsonUtils.getJsonNodeFromString;

/**
 * Object to parse and store the replication response
 */
public class ReplicationResponse {
    private int code;
    private String message;
    private String artifactId;

    public static ReplicationResponse from(SlingHttpResponse response) {
        ReplicationResponse res = new ReplicationResponse();
        res.setCode(response.getStatusLine().getStatusCode());
        try {
            JsonNode jsonNode = getJsonNodeFromString(response.getContent());
            parseJson(res, jsonNode);
            return res;
        } catch (ClientException e) {
            // Switch to throwing error once json is returned
        }
        // Is html
        res.setMessage(response.getContent());
        return res;
    }

    public static void parseJson(ReplicationResponse res, JsonNode jsonNode) {
        res.setMessage(jsonNode.path("status.message").asText());
        JsonNode artifactId = jsonNode.path("artifactId");
        if (!artifactId.isMissingNode()) {
            JsonNode path = artifactId.path(0);
            if (!path.isMissingNode()) {
                res.setId(path.asText());
            }
        }
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    private void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return artifactId != null ? artifactId : "";
    }

    private void setId(String artifactId) {
        this.artifactId = artifactId;
    }

    public int getCode() {
        return code;
    }

    private void setCode(int code) {
        this.code = code;
    }

    @Override 
    public String toString() {
        return "ReplicationResponse{code=" + code + ", message=\"" + StringUtils.normalizeSpace(message) + "\", artifactId=\"" + artifactId
            + "\"}";
    }
}