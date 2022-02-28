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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Package {

    private static final String ERROR = "ERROR";
    
    @JsonProperty("size")
    private int size;

    @JsonProperty("paths")
    private List<String> paths = new ArrayList<>();

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("action")
    private String action;
   
    @JsonProperty("id")
    private String id;
   
    @JsonProperty("pkgId")
    private String pkgId;
   
    @JsonProperty("time")
    private String time;
   
    @JsonProperty("state")
    private String state;
   
    @JsonProperty("userid")
    private String userid;
   
    @JsonProperty("attempts")
    private int attempts;
    
    @JsonIgnore
    public boolean isBlocked() {
        return state.equalsIgnoreCase(ERROR);
    }
    
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getPkgId() {
        return pkgId;
    }

    public void setPkgId(String pkgId) {
        this.pkgId = pkgId;
    }
    
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
        }
        return "";
    }
    
    public static Package fromJson(JsonNode node) {
        Package pkg = new Package();
        
        if (node != null) {
            pkg.setSize(node.path("size").asInt());

            List<String> paths = new ArrayList<>();
            for (JsonNode pathNode : node.path("paths")) {
                paths.add(pathNode.asText());
            }
            pkg.setPaths(paths);
            pkg.setAction(node.path("action").asText(""));
            pkg.setId(node.path("id").asText(""));
            pkg.setPkgId(node.path("pkgId").asText(""));
            pkg.setTime(node.path("time").asText(""));
            pkg.setState(node.path("state").asText(""));
            pkg.setAttempts(node.path("attempts").asInt());
            pkg.setErrorMessage(node.path("errorMessage").asText(""));
            pkg.setUserid(node.path("userid").asText(""));
        }
        return pkg;
    }
}
