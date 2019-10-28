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

package com.adobe.cq.cloud.testing.it.cf.smoke;

import com.adobe.cq.cloud.testing.it.cf.smoke.rules.CleanUpRule;
import com.adobe.cq.cloud.testing.it.cf.smoke.rules.ContentFragmentRule;
import com.adobe.cq.cloud.testing.it.cf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.sling.testing.clients.ClientException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class CFSmokeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFSmokeIT.class);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.cf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    private static final String TEST_CONTENT_FRAGMENT_PATH = "/content/dam/cfm-sanity-test/en/sample-structured";
    private static final String TEST_CONTENT_FRAGMENT_PARENT_PATH = "/content/dam/cfm-sanity-test/en/";
    private static final String TEST_CONTENT_FRAGMENT_SIMPLE_TEMPLATE = "/conf/cfm-sanity-test/settings/dam/cfm/templates/cfm-sanity-test/jcr:content";
    private static final String TEST_CONTENT_FRAGMENT_COMPLEX_TEMPLATE_PATH = "/conf/cfm-sanity-test/settings/dam/cfm/models/simple-structure";

    private static final String TEST_CONTENT_FRAGMENT_DESCRIPTION = "Test Content Fragment used to test the creation of a Content Fragment.";

    // Class rules that install packages
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();
    public static InstallPackageRule installPackageRule = new InstallPackageRule(cqBaseClassRule.authorRule, "/test-content", PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqBaseClassRule).around(installPackageRule);

    // Rule chain for clean up and content fragment utilities
    public CQRule cqRule = new CQRule();
    public ContentFragmentRule contentFragmentRule = new ContentFragmentRule(cqBaseClassRule.authorRule);
    public CleanUpRule cleanUpRule = new CleanUpRule(cqBaseClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    @Rule
    public TestRule rules = RuleChain.outerRule(cqRule).around(cleanUpRule).around(contentFragmentRule);

    @Test
    public void testCreateContentFragmentWithSimpleModel() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Test Create Content Fragment with a simple model.");
        testCreateContentFragment(TEST_CONTENT_FRAGMENT_SIMPLE_TEMPLATE);
        LOGGER.info("Content Fragment was created successfully.");
    }

    @Test
    public void testCreateContentFragmentWithComplexModel() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Test Create Content Fragment with a complex model.");
        testCreateContentFragment(TEST_CONTENT_FRAGMENT_COMPLEX_TEMPLATE_PATH);
        LOGGER.info("Content Fragment was created successfully.");
    }

    @Test
    public void testCreateContentFragmentModel() {

    }

    @Test
    public void testCreateContentFragmentVariation() {

    }

    private void testCreateContentFragment(String templatePath) throws ClientException, TimeoutException, InterruptedException {
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        String testContentFragmentTitle = "test-content-fragment-title-" + UUID.randomUUID();
        String testContentFragmentName = "test-content-fragment-name-" + UUID.randomUUID();

        final String path = contentFragmentRule.createContentFragment(
                TEST_CONTENT_FRAGMENT_PARENT_PATH,
                templatePath,
                testContentFragmentTitle,
                testContentFragmentName,
                TEST_CONTENT_FRAGMENT_DESCRIPTION
        );
        cleanUpRule.addPath(path);
    }

}
