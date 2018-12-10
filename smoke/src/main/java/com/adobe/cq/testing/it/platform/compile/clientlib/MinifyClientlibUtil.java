/*
 * Copyright 2018 Adobe Systems Incorporated
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
package com.adobe.cq.testing.it.platform.compile.clientlib;

import com.adobe.cq.testing.client.CQClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.query.QueryClient;
import org.apache.sling.testing.clients.util.HttpUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MinifyClientlibUtil {
    private static final Logger LOG = LoggerFactory.getLogger(MinifyClientlibUtil.class);

    protected static final String CLIENTLIB_QUERY = "SELECT * FROM [cq:ClientLibraryFolder] AS s " +
            "WHERE ISDESCENDANTNODE([{{path}}]) option(traversal ok)";

    /**
     * Find all {@code cq:ClientLibraryFolder} nodes below a path
     *
     * @param path where to search
     * @return results as returned by {@code QueryClient#doQuery()}
     * @throws ClientException if the request fails
     * @throws InterruptedException if interrupted while waiting for the server
     */
    protected static JsonNode searchClientLibFolders(CQClient client, String path)
            throws ClientException, InterruptedException {
        final String query = CLIENTLIB_QUERY.replace("{{path}}", path);
        return client.adaptTo(QueryClient.class).doQuery(query, QueryClient.QueryType.SQL2);
    }

    /**
     * Loop through json result and find pages which show compile errors in minified version
     *
     * @param searchJson search result
     * @param ignorePaths list of paths to be ignored when iterating over results
     * @return list of failures
     * @throws ClientException if any request fails
     */
    protected static List<String> getFailures(CQClient client, JsonNode searchJson, List<String> ignorePaths)
            throws ClientException {

        List<String> failures = new ArrayList<>();
        JsonNode results = searchJson.get("results");
        LOG.info("[{}] found {} CQ pages to check", client.getUrl(), results.size());
        int i = 0;
        for (JsonNode result: results) {
            // get the path to check
            String path = result.get("path").getTextValue();

            // check if its not to be ignored
            if (ignorePaths.contains(path)) {
                LOG.info("[{}] Path ignored: {}", client.getUrl(), path);
                continue;
            }

            // check first if default selector works ok
            int status = HttpUtils.getHttpStatus(client.doGet(path + ".js"));
            if (status == 404) {
                LOG.debug("[{}] clientlib not found: {}", client.getUrl(), path);
                continue;
            }

            SlingHttpResponse minifiedResponse = client.doGet(path + ".min.js");
            if (status == 200 && HttpUtils.getHttpStatus(minifiedResponse) != 200) {
                failures.add(path);
            } else {
                LOG.debug("[{}] clientlib OK: {}.min.js", client.getUrl(), path);
            }

            // long running so we print the progress from time to time
            if ((i + 1) % 100 == 0 || i == results.size() - 1) {
                LOG.info("[{}] Checked: {}/{} ({})", client.getUrl(), i + 1, results.size(), path);
            }
            i++;
        }

        return failures;
    }
}
