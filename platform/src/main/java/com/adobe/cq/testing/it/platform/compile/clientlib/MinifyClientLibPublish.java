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
import com.adobe.cq.testing.junit.rules.CQPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.junit.rules.category.SmokeTest;
import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Category(SmokeTest.class)
public class MinifyClientLibPublish {
    private static final Logger LOG = LoggerFactory.getLogger(MinifyClientLibPublish.class);

    @ClassRule
    public static CQPublishClassRule cqBaseClassRule = new CQPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule();

    @Test
    public void testMinifyClientLibPublish() throws ClientException, InterruptedException {
        LOG.info("start testMinifyClientLibPublish");

        // Ignore handles in check
        final List<String> ignorePaths = Collections.emptyList();

        CQClient client = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);

        JsonNode searchResult = MinifyClientlibUtil.searchClientLibFolders(client, "/");

        // loop through result and find js which shows compile errors
        List<String> list = MinifyClientlibUtil.getFailures(client, searchResult, ignorePaths);

        Assert.assertTrue("[publish] Compile errors in minified version for " + list.size() + " js file(s): \n" +
                StringUtils.join(list, "\n"), list.isEmpty());

        LOG.info("finish testMinifyClientLibPublish");
    }
}
