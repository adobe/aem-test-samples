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
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.util.LoginUtil;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.junit.rules.category.SmokeTest;
import org.codehaus.jackson.JsonNode;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple smoke tests to check roughly if cq pages throw a JSP compile error
 */
@Category(SmokeTest.class)
public class JSPCompileErrorAuthor {

    @ClassRule
    public static CQAuthorClassRule cqAuthorBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule();

    private String loginToken;
    private CQClient adminAuthor;

    @Before
    public void prepareLogin() throws IOException {
        adminAuthor = cqAuthorBaseClassRule.authorRule.getAdminClient(CQClient.class);
        loginToken = LoginUtil.getLoginToken(adminAuthor, "");
    }

    /**
     * Check JSP compile errors in login page
     *
     * @throws ClientException
     */
    @Test
    public void testLogin() throws ClientException {
        SlingHttpResponse exec = adminAuthor.doGet("/libs/granite/core/content/login.html");

        Assert.assertFalse("Login page has JSP Compile errors!", JSPCompileUtil.checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in crxde
     *
     * @throws ClientException
     */
    @Test
    public void testCRXDE() throws ClientException {
        SlingHttpResponse exec = adminAuthor.doGet("/crx/de/index.jsp");

        Assert.assertFalse("crxde has JSP Compile errors!",
                JSPCompileUtil.checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in crx explorer
     *
     * @throws ClientException
     */
    @Test
    public void testCRXExplorer() throws ClientException {
        SlingHttpResponse exec = adminAuthor.doGet("/crx/explorer/index.jsp");

        Assert.assertFalse("crx explorer has JSP Compile errors!",
                JSPCompileUtil.checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in package manager
     *
     * @throws ClientException
     */
    @Test
    public void testPackageManager() throws ClientException {
        SlingHttpResponse exec = adminAuthor.doGet("/crx/packmgr/index.jsp");

        Assert.assertFalse("PackageManager has JSP Compile errors!",
                JSPCompileUtil.checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in backup admin page
     *
     * @throws ClientException
     */
    @Test
    public void testBackupAdmin() throws ClientException {
        SlingHttpResponse exec = adminAuthor.doGet("/libs/granite/backup/content/admin.html");

        Assert.assertFalse("Backup admin page has JSP Compile errors!",
                JSPCompileUtil.checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in granite security admin
     *
     * @throws ClientException
     */
    @Test
    public void testSecurityAdminGranite() throws ClientException {
        SlingHttpResponse exec = adminAuthor.doGet("/libs/granite/security/content/admin.html");

        Assert.assertFalse("Granite Security Admin page has JSP Compile errors!",
                JSPCompileUtil.checkHtmlForCompileError(exec.getContent()));
    }

    /**
     * Check JSP compile errors in console pages
     *
     * @throws ClientException
     */
    @Test
    public void testConsoles() throws ClientException, InterruptedException {
        final String path = "/libs";
        JsonNode consoles = JSPCompileUtil.searchCQConsoles(adminAuthor, path);

        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = JSPCompileUtil.getFailures(adminAuthor, loginToken, consoles);

        Assert.assertEquals("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + JSPCompileUtil.formatFailureString(list), 0, list.size());
    }

    /**
     * Check JSP compile errors in libs pages
     *
     * @throws ClientException
     */

    @Test
    public void testLibs() throws ClientException, InterruptedException {
        JsonNode pages = JSPCompileUtil.searchCQPages(adminAuthor, "/libs");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();
        // JSP Compile Errors in /libs/wcm/foundation/components/pagetypes/html5page
        // as these are prototypes for author templated pages these pages are not complete in their definition and
        // therefore don't have to be compiled
        ignoreHandles.add("/libs/settings/wcm/template-types/html5page/initial");
        ignoreHandles.add("/libs/settings/wcm/template-types/html5page/structure");
		
		// To avoid JSP Compile Errors in /libs/wcm/foundation/components/pagetypes/afpage.
		// As its prototype of AF template and not an actual AF template
        ignoreHandles.add("/libs/settings/wcm/template-types/afpage/initial");
        ignoreHandles.add("/libs/settings/wcm/template-types/afpage/structure");

        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = JSPCompileUtil.getFailures(adminAuthor, loginToken, pages, ignoreHandles);

        Assert.assertEquals("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + JSPCompileUtil.formatFailureString(list), 0, list.size());
    }

    /**
     * Check JSP compile errors in apps pages
     *
     * @throws ClientException
     */
    @Test
    public void testApps() throws ClientException, InterruptedException {
        JsonNode pages = JSPCompileUtil.searchCQPages(adminAuthor, "/apps");

        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = JSPCompileUtil.getFailures(adminAuthor, loginToken, pages);

        Assert.assertEquals("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + JSPCompileUtil.formatFailureString(list), 0, list.size());
    }

    /**
     * Check JSP compile errors in etc pages
     *
     * @throws ClientException
     */
    @Test
    public void testEtc() throws ClientException, InterruptedException {
        JsonNode pages = JSPCompileUtil.searchCQPages(adminAuthor, "/etc");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();
        // see https://issues.adobe.com/browse/CQ5-27222 - NPE / JSP Compile error in
        // /apps/geometrixx-outdoors/components/activitystreams/communitystreams/useractivities/useractivities.jsp
        // issue is Deferred
        ignoreHandles.add("/content/communities/content-driven/en");
        ignoreHandles.add("/content/communities/content-driven/en/activity");
        ignoreHandles.add("/content/communities/discussion-driven/en/activity");
        ignoreHandles.add("/content/communities/event-driven/en");
        ignoreHandles.add("/content/communities/event-driven/en/activity");

        // loop through result and find pages which shows JSP compile errors
        Map<String, List<String>> list = JSPCompileUtil.getFailures(adminAuthor, loginToken, pages, ignoreHandles);

        Assert.assertEquals("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + JSPCompileUtil.formatFailureString(list), 0, list.size());
    }


    /**
     * Check JSP compile errors in content pages
     *
     * @throws ClientException
     */
    @Test
    public void testContent() throws ClientException, InterruptedException {
        long start = System.currentTimeMillis();
        JsonNode pages = JSPCompileUtil.searchCQPages(adminAuthor, "/content");

        // Ignore handles in check
        ArrayList<String> ignoreHandles = new ArrayList<>();

        ignoreHandles.add("/content/mac/default/tools/accountslinking");
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
        Map<String, List<String>> list = JSPCompileUtil.getFailures(adminAuthor, loginToken, pages, ignoreHandles);

        Assert.assertEquals("Following " + list.size() + " template(s) contains components with JSP Compile errors in " +
                "following handles: \n" + JSPCompileUtil.formatFailureString(list), 0, list.size());
    }

}
