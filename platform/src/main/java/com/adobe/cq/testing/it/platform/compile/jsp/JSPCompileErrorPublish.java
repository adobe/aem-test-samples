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
import com.adobe.cq.testing.junit.rules.CQPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.util.LoginUtil;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple smoke tests to check roughly if cq pages throw a JSP compile error
 */
public class JSPCompileErrorPublish extends JSPCompileUtil {

    @ClassRule
    public static CQPublishClassRule cqPublishBaseClassRule = new CQPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule();

    private String loginToken;
    private CQClient anonymousPublish;
    private CQClient adminPublish;

    @Before
    public void prepareLogin() throws IOException {
        anonymousPublish = cqPublishBaseClassRule.publishRule.getClient(CQClient.class, "anonymous", "anonymous");
        adminPublish = cqPublishBaseClassRule.publishRule.getAdminClient(CQClient.class);

        loginToken = LoginUtil.getLoginToken(anonymousPublish, "");
    }

    /**
     * Check JSP compile errors in login page
     *
     * @throws ClientException
     */
    @Test
    public void testLogin() throws ClientException {
        SlingHttpResponse exec = anonymousPublish.doGet("/libs/granite/core/content/login.html");

        Assert.assertFalse("Login page has JSP Compile errors!", checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in libs pages
     *
     * @throws ClientException
     */
    @Test
    public void testLibs() throws ClientException, InterruptedException {
        JsonNode exec = searchCQPages(adminPublish, "/libs");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();
        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = getFailures(anonymousPublish, loginToken, exec, ignoreHandles);

        Assert.assertTrue("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + formatFailureString(list),
                list.size() == 0);
    }

    /**
     * Check JSP compile errors in apps pages
     *
     * @throws ClientException
     */
    @Test
    public void testApps() throws ClientException, InterruptedException {
        JsonNode results = searchCQPages(adminPublish, "/apps");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();
        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = getFailures(anonymousPublish, loginToken, results, ignoreHandles);

        Assert.assertTrue("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + formatFailureString(list),
                list.size() == 0);
    }

    /**
     * Check JSP compile errors in etc pages
     *
     * @throws ClientException
     */
    @Test
    public void testEtc() throws ClientException, IOException, InterruptedException {
        JsonNode results = searchCQPages(adminPublish, "/etc");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();
        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = getFailures(anonymousPublish, loginToken, results, ignoreHandles);

        Assert.assertTrue("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + formatFailureString(list),
                list.size() == 0);
    }


    /**
     * Check JSP compile errors in content pages
     *
     * @throws ClientException
     */
    @Test
    public void testContent() throws ClientException, InterruptedException {
        JsonNode results = searchCQPages(adminPublish, "/content");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();

        // skip communities' templates
        ignoreHandles.add("/content/communities/templates/basetemplate/profile");
        ignoreHandles.add("/content/communities/templates/basetemplate/messaging/trash");
        ignoreHandles.add("/content/communities/templates/basetemplate/messaging/sent");
        ignoreHandles.add("/content/communities/templates/basetemplate/messaging/compose");
        ignoreHandles.add("/content/communities/templates/basetemplate/messaging");
        ignoreHandles.add("/content/communities/templates/basetemplate/search");
        ignoreHandles.add("/content/communities/templates/basetemplate/signup");
        ignoreHandles.add("/content/communities/templates/basetemplate/signin");
        ignoreHandles.add("/content/communities/templates/basetemplate/notifications");
        ignoreHandles.add("/content/communities/templates/functions/activitystream");
        ignoreHandles.add("/content/communities/templates/functions/assignments");
        ignoreHandles.add("/content/communities/templates/functions/blog");
        ignoreHandles.add("/content/communities/templates/functions/calendar");
        ignoreHandles.add("/content/communities/templates/functions/catalog");
        ignoreHandles.add("/content/communities/templates/functions/filelibrary");
        ignoreHandles.add("/content/communities/templates/functions/forum");
        ignoreHandles.add("/content/communities/templates/functions/groups");
        ignoreHandles.add("/content/communities/templates/functions/page");
        ignoreHandles.add("/content/communities/templates/functions/qna");
        ignoreHandles.add("/content/communities/templates/grouptemplate/members");
        ignoreHandles.add("/content/geometrixx-outdoors/en/user/smartlist");

        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = getFailures(anonymousPublish, loginToken, results, ignoreHandles);

        Assert.assertTrue("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + formatFailureString(list),
                list.size() == 0);

    }
}
