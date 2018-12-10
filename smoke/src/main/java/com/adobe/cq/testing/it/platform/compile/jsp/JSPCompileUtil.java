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
package com.adobe.cq.testing.it.platform.compile.jsp;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.util.LoginUtil;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.query.QueryClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSPCompileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(JSPCompileUtil.class);

    protected static final String CRX_DE_QUERY_PATH = "/crx/de/query.jsp";
    protected static final String PAGES_QUERY = "SELECT * FROM [cq:Page] AS s WHERE ISDESCENDANTNODE([{{path}}]) option(traversal ok)";
    protected static final String CONSOLES_QUERY = "SELECT * FROM [cq:Console] AS s WHERE ISDESCENDANTNODE([{{path}}]) option(traversal ok)";

    /**
     * Check for compile error in given html content
     *
     * @param htmlContent content string
     * @return true if the string contains ScriptEvaluationException
     */
    protected static boolean checkHtmlForCompileError(String htmlContent) {
        return htmlContent.contains("ScriptEvaluationException");
    }

    /**
     * Find all pages below a certain path.
     *
     * @param client http client to be used
     * @param path path to be queried for cq pages
     * @return the results json
     * @throws ClientException if the request fails
     */
    protected static JsonNode searchCQPages(SlingClient client, String path) throws ClientException, InterruptedException {
        final String query = PAGES_QUERY.replace("{{path}}", path);
        return client.adaptTo(QueryClient.class).doQuery(query, QueryClient.QueryType.SQL2);
    }

    /**
     * Find all consoles below a certain path.
     *
     * @param client http client to be used
     * @param path path to be queried for cq pages
     * @return the results json
     * @throws ClientException if the request fails
     */
    protected static JsonNode searchCQConsoles(SlingClient client, String path) throws ClientException, InterruptedException {
        final String query = CONSOLES_QUERY.replace("{{path}}", path);
        return client.adaptTo(QueryClient.class).doQuery(query, QueryClient.QueryType.SQL2);
    }

    /**
     * Loop through json result and find pages which shows JSP compile errors
     *
     * @param cqClient cq client to work with
     * @param loginToken login token
     * @param pages search result in json
     * @param ignoreHandles ignore handles while check
     * @return handles which contains JSP compile errors grouped by templates
     * @throws ClientException if the requests fail
     */
    protected static Map<String, List<String>> getFailures(CQClient cqClient, String loginToken, JsonNode pages,
                                                           ArrayList<String> ignoreHandles)
            throws ClientException {

        // key = template name, value = list of failing pages that use this template
        Map<String, List<String>> list = new LinkedHashMap<>();

        JsonNode results = pages.get("results");
        LOG.info("[{}] found {} CQ pages to check", cqClient.getUrl(), results.size());
        int i = 0;
        for (JsonNode result: results) {
            // get the path to check
            String path = result.get("path").getTextValue();

            // skip if its in the list of ignored
            if (ignoreHandles.contains(path)) {
                LOG.info("[{}] path ignored: {}", cqClient.getUrl(), path);
                continue;
            }

            // access page and check html for errors
            HttpResponse response = null;
            String content = null;
            try {
                response = LoginUtil.doGetWithLoginToken(loginToken, cqClient, path + ".html");
                content = EntityUtils.toString(response.getEntity());
                // consume response so that the connection can be reused
                EntityUtils.consume(response.getEntity());
            } catch (Exception e) {
                LOG.info("Error happened when loading " + path, e);
            }

            if (response == null || checkHtmlForCompileError(content)) {

                // group by template
                String template = (result.get("cq:template") != null)
                        ? "Template: " + result.get("cq:template").getTextValue()
                        : "[unknown template]";
                List<String> paths = new ArrayList<>();
                if (list.containsKey(template)) {
                    paths = list.get(template);
                } else {
                    list.put(template, paths);
                }

                if (content != null) {
                    LOG.info("ScriptEvaluationException found in: {}", path);
                    paths.add(path + "\n" + getExceptionOutput(content));
                } else {
                    paths.add(path + "\n=> An exception was thrown when loading this page");
                }
            }

            // long running so we print the progress from time to time
            if ((i + 1) % 100 == 0 || i == results.size() - 1) {
                LOG.info("[{}] Checked: {}/{} ({})", cqClient.getUrl(), i + 1, results.size(), path);
            }
            i++;
        }

        return list;
    }

    protected static final Pattern p1 = Pattern.compile(".*(Error during include of component[[^<]|.]*).*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    protected static final Pattern p2 = Pattern.compile(".*(ScriptEvaluationException:[[^\n]|[^\r\n]|.]*).*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    protected static final Pattern p3 = Pattern.compile(".*(Caused by:[[^<]|.]*).*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    // extract the error output from the HTML
    protected static String getExceptionOutput(String content) {

        // look for the 'Error during include' string
        StringBuilder errorString = new StringBuilder("=> ");
        Matcher m = p1.matcher(content);
        if (m.matches()) {
            errorString.append(m.group(1));
        }

        // extract the ScriptEvaluationException with the error line
        m = p2.matcher(content);
        if (m.matches()) {
            errorString.append("\n").append(m.group(1));
        }

        // extract the root cause
        m = p3.matcher(content);
        if (m.matches()) {
            errorString.append("\n").append(m.group(1));
        }


        return errorString.toString();
    }

    /**
     * Loop through json result and find pages which shows JSP compile errors
     *
     * @param cqClient cq client to work with
     * @param loginToken login token
     * @param pages search result in json
     * @return handles which contains JSP compile errors grouped by templates
     * @throws ClientException
     */
    protected static Map<String, List<String>> getFailures(CQClient cqClient, String loginToken, JsonNode pages)
            throws ClientException {
        return getFailures(cqClient, loginToken, pages, new ArrayList<String>());
    }

    /**
     * Format failure String for better readability
     *
     * @param list list of failures
     * @return the formatted string
     */
    protected static String formatFailureString(Map<String, List<String>> list) {
        int i;
        String failureString = "";
        if (list.size() > 0) {
            for (String template : list.keySet()) {
                failureString += "\n" + template + ":\n";
                ArrayList<String> paths = (ArrayList<String>) list.get(template);
                for (i = 0; i < paths.size(); i++) {
                    failureString += "   " + paths.get(i) + "\n";
                }
            }
        }
        return failureString;
    }
}
