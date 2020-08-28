/*
 * Copyright 2019 Adobe Systems Incorporated
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
package com.adobe.cq.cloud.testing.it.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;

public class GetTogglesIT {

    @ClassRule
    public static CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();
    static CQClient adminAuthor;
    static CQClient adminPublish;
    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);
    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
    }

    /**
     * Verifies if the "ENABLED" flag is always enabled on Author Instance
     * Considers the test failed if the answer can't be parsed.
     *
     * @throws ClientException if an error occurred
     */
    @Test
    public void testTogglesEndpointReturnsStaticEnabledFlagInJsonResponseOnAuthor() throws ClientException {
        sharedTestResponseAlwaysContainsEnabledFlag(adminAuthor);
    }

    /**
     * Verifies if the "ENABLED" flag is always enabled on Publish Instance
     * Considers the test failed if the answer can't be parsed.
     *
     * @throws ClientException if an error occurred
     */
    @Test
    @Ignore
    public void testTogglesEndpointReturnsStaticEnabledFlagInJsonResponseOnPublish() throws ClientException {
        sharedTestResponseAlwaysContainsEnabledFlag(adminPublish);
    }

    /**
     * Validate format of AEM version containing state qualifier using six digits
     *
     * @throws Exception if an error occurred
     */
    @Test
    public void testAboutPageVersionFormatWithToggleQualifier() throws Exception {
        SlingHttpResponse response = adminAuthor.doGet("mnt/overlay/granite/ui/content/shell/about.html", 200);
        final String regex = "^Adobe Experience Manager [\\d]{4}.[\\d]{1,2}.[\\d]+.[\\d]{8}T[\\d]{6}Z-[\\d]{6}$";

        String content = response.getContent()
                .replaceAll("<!DOCTYPE((.|\n|\r)*?)>", "")
                .replaceAll("<!ENTITY ((.|\n|\r)*?)\">", "");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        Document doc =  dbf.newDocumentBuilder()
            .parse(new InputSource(new StringReader(content)));

        Element parentElement = doc.getDocumentElement();
        boolean matchFound = checkElementMatchesRegex(regex, parentElement);

        Assert.assertTrue("version regex " + regex + " not matching content in about page \n" + content, matchFound);
    }

    private boolean checkElementMatchesRegex(String regex, Node parentElement) {
        boolean match = false;
        if (parentElement.hasChildNodes()) {
            NodeList childNodes = parentElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node item = childNodes.item(i);
                String elementText = item.getTextContent().trim();
                if (elementText.matches(regex)) {
                    match = true;
                    break;
                } else {
                    match = match || checkElementMatchesRegex(regex, item);
                    if (match) {
                        break;
                    }
                }
            }
        }
        return match;
    }

    private void sharedTestResponseAlwaysContainsEnabledFlag(CQClient cqClient) throws ClientException {
        ObjectMapper mapper = new ObjectMapper();
        SlingHttpResponse response = cqClient.doGet("etc.clientlibs/toggles.json", 200);
        String responseContent = response.getContent();
        try {
            ToggleResponse tr = mapper.readValue(responseContent, ToggleResponse.class);
            Assert.assertTrue(Arrays.asList(tr.enabled).contains("ENABLED"));

        } catch (IOException e) {
            Assert.fail("Couldn't read response from ClientLibs toggle endpoint. \nError: " + e.getMessage() + "\nContents: " + responseContent);
        }
    }

    public static final class ToggleResponse {

        public String[] enabled;
    }
}
