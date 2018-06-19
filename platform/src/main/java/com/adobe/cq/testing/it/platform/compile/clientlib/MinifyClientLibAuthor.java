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
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
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
public class MinifyClientLibAuthor {
    private static final Logger LOG = LoggerFactory.getLogger(MinifyClientLibAuthor.class);

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule();

    @Test
    public void testMinifyClientLibAuthor() throws ClientException, InterruptedException {
        LOG.info("start testMinifyClientLibAuthor");

        // Ignore handles in check
        final List<String> ignorePaths = Collections.emptyList();

        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        JsonNode searchResult = MinifyClientlibUtil.searchClientLibFolders(client, "/");

        // loop through result and find js which shows compile errors
        List<String> list = MinifyClientlibUtil.getFailures(client, searchResult, ignorePaths);

        Assert.assertTrue("[author] Compile errors in minified version for " + list.size() + " js file(s): \n" +
                StringUtils.join(list, "\n"), list.isEmpty());

        LOG.info("finish testMinifyClientLibAuthor");
    }
}
