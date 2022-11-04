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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Queue {
    @JsonProperty
    private String name;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("itemsCount")
    private int itemsCount;
    
    @JsonProperty("empty")
    private boolean empty;
    
    @JsonProperty("packages")
    private Map<String, Package> packageMap = new HashMap<>();

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }
    
    private void setPackage(String id, Package pkg) {
        packageMap.put(id, pkg);
    }

    public Map<String, Package> getPackageMap() {
        return packageMap;
    }

    @SuppressWarnings("unused")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @SuppressWarnings("unused")
    public int getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Integer itemsCount) {
        this.itemsCount = itemsCount;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(Boolean empty) {
        this.empty = empty;
    }

    public static Queue fromJson(JsonNode node) {
        Queue queue = new Queue();
        
        if (node != null) {
            queue.setState(node.path("state").asText(""));
            queue.setItemsCount(node.path("itemsCount").asInt());
            queue.setEmpty(node.path("empty").asBoolean(true));

            //Iterator<JsonNode> itemsIterator = node.path("items").elements();
            
            Iterator<String> fieldNames = node.fieldNames();
            List<String> fieldList = new ArrayList<>();
            fieldNames.forEachRemaining(fieldList::add);
            List<String> pkgs =
                fieldList.stream().filter(s -> s.startsWith("package")).collect(Collectors.toList());

            for (String pkg : pkgs) {
                queue.setPackage(pkg, Package.fromJson(node.path(pkg)));
            }
        }
        return queue;
    }
}
