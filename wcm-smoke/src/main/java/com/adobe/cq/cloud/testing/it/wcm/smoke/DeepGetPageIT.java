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
package com.adobe.cq.cloud.testing.it.wcm.smoke;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DeepGetPageIT {

    @ClassRule
    public static final CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    private static HtmlUnitClient adminAuthor;
    private static HtmlUnitClient adminPublish;

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(HtmlUnitClient.class);
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(HtmlUnitClient.class);
    }

    @AfterClass
    public static void afterClass() {
        closeClientQuietly(adminAuthor);
        closeClientQuietly(adminPublish);
    }

    /**
     * Closes a client resource if applicable without notice in case it fails.
     * INFO: Used instead of org.apache.commons.io.IOUtils.closeQuietly since it is still marked as deprecated in
     * AEM, although it was un-deprecated in recent versions of commons-io (see <a href="https://issues.apache.org/jira/browse/IO-504">IO-504</a>)
     * @param client client resource.
     */
    private static void closeClientQuietly(HtmlUnitClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Verifies that the homepage exists on author
     *
     * @throws ClientException if an error occurred
     * @throws IOException if an error occurred
     * @throws URISyntaxException if an error occurred
     */
    @Test
    public void testHomePageAuthor() throws ClientException, IOException, URISyntaxException {
        verifyPageAndResources(adminAuthor, "/");
    }

    /**
     * Verifies that the sites console exists on author
     *
     * @throws ClientException if an error occurred
     * @throws IOException if an error occurred
     * @throws URISyntaxException if an error occurred
     */
    @Test
    public void testSitesAuthor() throws ClientException, IOException, URISyntaxException {
        verifyPageAndResources(adminAuthor, "/sites.html");
    }

    /**
     * Verifies that the assets console exists on author
     *
     * @throws ClientException if an error occurred
     * @throws IOException if an error occurred
     * @throws URISyntaxException if an error occurred
     */
    @Test
    public void testAssetsAuthor() throws ClientException, IOException, URISyntaxException {
        verifyPageAndResources(adminAuthor, "/assets.html");
    }

    /**
     * Verifies that the projects console exists on author
     *
     * @throws ClientException if an error occurred
     * @throws IOException if an error occurred
     * @throws URISyntaxException if an error occurred
     */
    @Test
    public void testProjectsAuthor() throws ClientException, IOException, URISyntaxException {
        verifyPageAndResources(adminAuthor, "/projects.html");
    }

    /**
     * Verifies that the homepage exists on publish
     *
     * @throws ClientException if an error occurred
     * @throws IOException if an error occurred
     * @throws URISyntaxException if an error occurred
     */
    @Test @Ignore
    public void testHomePagePublish() throws ClientException, IOException, URISyntaxException {
        verifyPageAndResources(adminPublish, "/");
    }

    //*********************************************
    // Internals
    //*********************************************

    /**
     * Verifies that specified page as well as the resources reference in it are available.
     * <P>
     * <b>Limitation:</b> Due to issues in HtmlUnit library javascript execution is disabled and as a result
     * only statically referenced resources are checked. All dynamically generated components
     * of the page are thus ignored.
     * @param client CQClient instance to use for accessing AEM.
     * @param path path to the tested page.
     * @throws ClientException if the request fails
     * @throws IOException if the resource references cannot be retrieved
     * @throws URISyntaxException if the uri cannot be parsed
     */
    private static void verifyPageAndResources(HtmlUnitClient client, String path) throws ClientException, IOException, URISyntaxException {
        URI baseURI = client.getUrl();
        for (URI ref : client.getResourceRefs(path)) {
            if (isSameOrigin(baseURI, ref)) {
                SlingHttpResponse response = client.doGet(ref.getRawPath());
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status returned from [" + ref + "]", 200, statusCode);
            }
        }
    }

    /** Checks if two URIs have the same origin
     * @param uri1 first URI
     * @param uri2 second URI
     * @return true if two URI come from the same host, port and use the same scheme
     */
    private static boolean isSameOrigin(URI uri1, URI uri2) {
        if (!uri1.getScheme().equals(uri2.getScheme())) {
            return false;
        }
        return uri1.getAuthority().equals(uri2.getAuthority());
    }

}
