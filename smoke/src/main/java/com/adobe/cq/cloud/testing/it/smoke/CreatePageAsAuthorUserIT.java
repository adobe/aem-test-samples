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
package com.adobe.cq.cloud.testing.it.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.security.CreateUserRule;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.sling.testing.clients.ClientException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;


import static java.util.concurrent.TimeUnit.MINUTES;

public class CreatePageAsAuthorUserIT {

    private static final long TIMEOUT = MINUTES.toMillis(3);
    public static final String CONTENT_AUTHORS_GROUP = "content-authors";

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);
    public CreateUserRule userRule = new CreateUserRule(cqBaseClassRule.authorRule, CONTENT_AUTHORS_GROUP);
    public Page pageRule = new Page(userRule.getClientSupplier());

    @Rule
    public TestRule cqRuleChain = RuleChain.outerRule(cqBaseRule).around(userRule).around(pageRule);


    /**
     * Verifies that a user belonging to the "Authors" group can create a page
     */
    @Test
    public void testCreatePageAsAuthor() throws ClientException, InterruptedException {
        // Assert page exists for admin
            Assert.assertTrue(cqBaseClassRule.authorRule.getAdminClient().adaptTo(CQClient.class)
                .pageExistsWithRetry(pageRule.getPath(), 10000));
        // This shows that it exists for the author user in a different way
        userRule.getClient().getAuthorSitesPage(pageRule.getPath(), 200);
    }

}
