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

import com.google.gson.Gson;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.*;

import java.util.Arrays;
import java.util.List;

public class GetTogglesIT {

    @ClassRule
    public static CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule, cqBaseClassRule.publishRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    static CQClient adminAuthor;
    static CQClient adminPublish;

    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
    }

    public final class ToggleResponse {
        public String[] enabled;
    }

    /**
     * Verifies if the "ENABLED" flag is always enabled on Author Instance
     */
    @Test
    public void testTogglesEndpointReturnsStaticEnabledFlagInJsonResponseOnAuthor() throws ClientException {
        sharedTestResponseAlwaysContainsEnabledFlag(adminAuthor);
    }

    /**
     * Verifies if the "ENABLED" flag is always enabled on Publish Instance
     */
    @Test
    public void testTogglesEndpointReturnsStaticEnabledFlagInJsonResponseOnPublish() throws ClientException {
        sharedTestResponseAlwaysContainsEnabledFlag(adminPublish);
    }

    private void sharedTestResponseAlwaysContainsEnabledFlag(CQClient cqClient) throws ClientException {
        SlingHttpResponse response = cqClient.doGet("/libs/system/toggles", 200);
        ToggleResponse tr = new Gson().fromJson(response.getContent(), ToggleResponse.class);
        List<String> _list = Arrays.asList(tr.enabled);
        assert _list.contains("ENABLED");
    }
}
