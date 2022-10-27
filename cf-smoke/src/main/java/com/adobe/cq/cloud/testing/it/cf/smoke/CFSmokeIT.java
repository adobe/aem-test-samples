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
import org.apache.sling.testing.clients.ClientException;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * A series of tests regarding the  creating of Content Fragment, Content Fragment Models and Content Fragments Variations.
 */
public class CFSmokeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFSmokeIT.class);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.cf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    private static final String TEST_CONTENT_FRAGMENT_FOLDER = "/content/dam/cf-sanity-test-20191029";
    private static final String TEST_CONTENT_FRAGMENT_CONF_FOLDER = "/conf/cf-sanity-test-20191029";
    private static final String TEST_CONTENT_FRAGMENT_PATH = "/content/dam/cf-sanity-test-20191029/en/sample-content-fragment-20191029";
    private static final String TEST_CONTENT_FRAGMENT_PARENT_PATH = "/content/dam/cf-sanity-test-20191029/en/";
    private static final String TEST_CONTENT_FRAGMENT_MODEL_PARENT_PATH = "/conf/cf-sanity-test-20191029/settings/dam/cf/models/";
    private static final String TEST_CONTENT_FRAGMENT_TEMPLATE = "/conf/cf-sanity-test-20191029/settings/dam/cf/templates/cf-sanity-test-20191029/jcr:content";
    private static final String TEST_CONTENT_FRAGMENT_CUSTOM_MODEL_PATH = "/conf/cf-sanity-test-20191029/settings/dam/cf/models/simple-structure-20191029";

    private static final String TEST_CONTENT_FRAGMENT_DESCRIPTION = "Test Content Fragment used to test the creation of a Content Fragment.";
    private static final String TEST_VARIATION_DESCRIPTION = "Content Fragment Test Variation.";

    // Class rules that install packages
    private static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();
    private static final InstallPackageRule installPackageRule = new InstallPackageRule(cqBaseClassRule.authorRule, "/test-content", PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static final TestRule ruleChain = RuleChain.outerRule(cqBaseClassRule).around(installPackageRule);

    // Rule chain for clean up and content fragment utilities
    private static final CQRule cqRule = new CQRule();
    private static final ContentFragmentRule contentFragmentRule = new ContentFragmentRule(cqBaseClassRule.authorRule);
    private static final CleanUpRule cleanUpRule = new CleanUpRule(cqBaseClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    @Rule
    public TestRule rules = RuleChain.outerRule(cqRule).around(cleanUpRule).around(contentFragmentRule);

    /**
     * As we creating content fragments in certain folders, the InstallPackageRule sometimes does not
     * remove the parent content fragment folder.
     * <p>
     * Through this after method, we make sure that the clean up is done properly.
     */
    @AfterClass
    public static void after() {
        try {
            CleanUpRule.cleanUp(cqBaseClassRule.authorRule, TEST_CONTENT_FRAGMENT_FOLDER, TIMEOUT, RETRY_DELAY);
        } catch (InterruptedException | TimeoutException | RuntimeException ignored) {}

        try {
            CleanUpRule.cleanUp(cqBaseClassRule.authorRule, TEST_CONTENT_FRAGMENT_CONF_FOLDER, TIMEOUT, RETRY_DELAY);
        } catch (InterruptedException | TimeoutException | RuntimeException ignored) {}
    }

    /**
     *  Testing to check if the creation of a content fragment with the default template works as intended.
     *
     * @throws ClientException - if there is an error while using CQClient
     * @throws TimeoutException - if a timeout occurs while checking the existence of a content fragment
     * @throws InterruptedException - if an error occurs while checking the existence of a content fragment
     */
    @Test
    public void testCreateContentFragmentWithTemplate() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Test Create Content Fragment with a simple model.");
        testCreateContentFragment(TEST_CONTENT_FRAGMENT_TEMPLATE);
        LOGGER.info("Content Fragment was created successfully.");
    }

    /**
     *  Testing to check if the creation of a content fragment with a custom model works as intended.
     *
     * @throws ClientException - if there is an error while using CQClient
     * @throws TimeoutException - if a timeout occurs while checking the existence of a content fragment
     * @throws InterruptedException - if an error occurs while checking the existence of a content fragment
     */
    @Test
    public void testCreateContentFragmentWithCustomModel() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Test Create Content Fragment with a complex model.");
        testCreateContentFragment(TEST_CONTENT_FRAGMENT_CUSTOM_MODEL_PATH);
        LOGGER.info("Content Fragment was created successfully.");
    }

    /**
     *  Testing to check if the creation of a content fragment model works as intended.
     *
     * @throws ClientException - if there is an error while using CQClient
     * @throws TimeoutException - if a timeout occurs while checking the existence of a content fragment model
     * @throws InterruptedException - if an error occurs while checking the existence of a content fragment model
     */
    @Test
    public void testCreateContentFragmentModel() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Test Create Content Fragment Model.");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        String contentFragmentModelName = "content-fragment-test-model-" + UUID.randomUUID();
        String contentFragmentModelDescription = "This is a test content fragment model.";

        String path = contentFragmentRule.createContentFragmentModel(
                TEST_CONTENT_FRAGMENT_MODEL_PARENT_PATH,
                contentFragmentModelName,
                contentFragmentModelDescription
        );
        cleanUpRule.addPath(path);

        client.waitExists(path, TIMEOUT, RETRY_DELAY);
        LOGGER.info("Content Fragment Model was created successfully.");
    }

    /**
     *  Given a Content Fragment, create a variation with a random name.
     *
     * @throws ClientException - if there is an error while using CQClient
     */
    @Test
    public void testCreateContentFragmentVariation() throws ClientException {
        LOGGER.info("Test Create Content Fragment Variation.");

        String variationName = "content-fragment-variation-" + UUID.randomUUID();

        // create a content fragment variation inside our test content fragment
        contentFragmentRule.createVariation(TEST_CONTENT_FRAGMENT_PATH, variationName, TEST_VARIATION_DESCRIPTION);

        LOGGER.info("Content Fragment variation was created successfully.");
    }

    /**
     *  Helper method for creating a content fragment with either a template or a custom model.
     *  It creates a Content Fragment with a random title and name, under the path defined
     *  by TEST_CONTENT_FRAGMENT_PARENT_PATH.
     *
     * @param templatePath - the path to the default template or the custom Content Fragment Model
     * @throws ClientException - if there is an error with the CQClient
     * @throws TimeoutException - if a timeout is reached while checking the existence of a Content Fragment
     * @throws InterruptedException - if an interruption occurs while checking the existence of a Content Fragment
     */
    private void testCreateContentFragment(String templatePath) throws ClientException, TimeoutException, InterruptedException {
        final CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

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

        client.waitExists(path, TIMEOUT, RETRY_DELAY);
    }

}
