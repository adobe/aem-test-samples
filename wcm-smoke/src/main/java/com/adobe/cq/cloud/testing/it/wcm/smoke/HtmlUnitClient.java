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

import com.gargoylesoftware.htmlunit.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClientConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.adobe.cq.testing.client.CQClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.sling.testing.clients.exceptions.TestingIOException;
import org.w3c.dom.Node;

import static org.junit.Assert.fail;

/**
 * AEM client that maintains a WebClient instance from HttpUnit framework
 */
public class HtmlUnitClient extends CQClient {

    private final WebClient webClient = new WebClient();

    /** Extracts references to external resources used by the specified page.
     * This method extracts references from script, img, meta and link tags.
     * @param path path to the page.
     * @return list of URIs resolved against the pages baseURL
     * @throws IOException when IO error occurs
     * @throws URISyntaxException if malformed URL reference is found.
     */
    public List<URI> getResourceRefs(String path) throws IOException, URISyntaxException {
        HtmlPage page = getPage(path, false);
        List<URI> result = new ArrayList<>();
        result.addAll(getRefs(page, "script", "src"));
        result.addAll(getRefs(page, "img", "src"));
        result.addAll(getRefs(page, "meta", "href"));
        result.addAll(getRefs(page, "link", "href"));
        return result;
    }

    /**
     * Loads html page specified by path.
     * @param path path to the page
     * @param javaScriptEnabled whether to execute javascript
     * @return parsed page.
     * @throws IOException if IO error occurs.
     */
    public HtmlPage getPage(String path, boolean javaScriptEnabled) throws IOException {
        WebClientOptions options = webClient.getOptions();
        boolean wasJsEnabled = options.isJavaScriptEnabled();
        try {
            options.setJavaScriptEnabled(javaScriptEnabled);
            return getPage(webClient, getUrl(path).toURL());
        } finally {
            options.setJavaScriptEnabled(wasJsEnabled);
        }
    }

    //*********************************************
    // Creation
    //*********************************************

    /**
     * Custom HTTP Web Connection which enables usage of system properties for proxy usage for HtmlUnit
     */
    class SmokeTestHttpWebConnection extends HttpWebConnection {

        public SmokeTestHttpWebConnection(WebClient webClient) {
            super(webClient);
        }

        @Override
        protected HttpClientBuilder createHttpClientBuilder() {
            return super.createHttpClientBuilder().useSystemProperties();
        }
    }

    public HtmlUnitClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
        URI baseUri = getUrl();
        AuthScope scope = new AuthScope(baseUri.getHost(), baseUri.getPort());
        DefaultCredentialsProvider credProvider = new DefaultCredentialsProvider();
        credProvider.addCredentials(getUser(), getPassword());
        Credentials creds = new UsernamePasswordCredentials(getUser(), getPassword());
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(scope, creds);
        webClient.setCredentialsProvider(credentialsProvider);
        webClient.setWebConnection(new SmokeTestHttpWebConnection(webClient));
        login(webClient);
    }

    @Override
    public void close() throws IOException {
        try {
            webClient.close();
        } finally {
            super.close();
        }
    }

    //*********************************************
    // Internals
    //*********************************************

    private static List<URI> getRefs(HtmlPage page, String tag, String refAttr) throws URISyntaxException {
        URI baseUri = new URI(page.getBaseURI());
        List<URI> result = new ArrayList<>();
        for (DomNode child : page.getElementsByTagName(tag)) {
            URI uriRef = getNamedItemAsUri(child, refAttr);
            if (uriRef != null) {
                URI uri = baseUri.resolve(uriRef);
                result.add(uri);
            }
        }
        return result;
    }

    /**
     *  Loads requested page while suppressing CSS errors (logged as warnings)
     * @param webClient web client to use for loading
     * @param url page URL
     * @return loaded HtmlPage instance.
     * @throws IOException when error occurs
     */
    private static HtmlPage getPage(WebClient webClient, URL url) throws IOException {
        Logger logger = Logger.getLogger(DefaultCssErrorHandler.class.getName());
        Level originalLevel = logger.getLevel();
        try {
            logger.setLevel(Level.SEVERE);
            return webClient.getPage(url);
        } finally {
            logger.setLevel(originalLevel);
        }
    }

    /**
     * Preemptively logins to the server
     * @param webClient web client to use for logging in.
     * @throws TestingIOException if the request failed
     */
    private void login(WebClient webClient) throws TestingIOException {
        URL loginUrl = null;
        try {
            loginUrl = getUrl("/bin/receive?sling:authRequestLogin=1").toURL();
            WebResponse response = webClient.loadWebResponse(new WebRequest(loginUrl));
            if (response.getStatusCode() != 200) {
                throw new TestingIOException("Unable to login to server: [" + loginUrl + "]. Unexpected status: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new TestingIOException("Unable to login to server: [" + loginUrl + "]", e);
        }
    }

    /**
     * Extracts URI reference from specified element and converts it to URI
     * This method will trigger junit assertion if refAttr value cannot be parsed as URI
     * providing comprehensive error message.
     * @param node - html element from which to extract reference
     * @param refAttr - name of the attribute containing corresponding value
     * @return refAttr value as URI or <code>null</code> if attribute does not exist
     */
    private static URI getNamedItemAsUri(DomNode node, String refAttr) {
        Node src = node.getAttributes().getNamedItem(refAttr);
        if (src == null) {
            return null;
        } else {
            try {
                String href = src.getNodeValue();
                return new URI(href);
            } catch (URISyntaxException e) {
                fail("Invalid URI value in [" + refAttr + "] attribute in: [" + node + "].\n" +
                        "   Page URL:  [" + node.getPage().getUrl() + "]\n" +
                        "   XPath:     [" + node.getCanonicalXPath() + "]\n" +
                        "   Caused by: [" + e.getMessage() + "]");
                throw new AssertionError(); // must never happen
            }
        }
    }

}
