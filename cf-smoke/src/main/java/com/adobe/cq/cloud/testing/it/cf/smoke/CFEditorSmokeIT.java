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
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

/**
 * Series of smoke tests that check if a Content Fragment Editor and Content Fragment Model Editor work
 * as intended.
 */
public class CFEditorSmokeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFEditorSmokeIT.class);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.cf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    private static final String TEST_CONTENT_FRAGMENT_FOLDER = "/content/dam/cf-sanity-test-20191029";
    private static final String TEST_CONTENT_FRAGMENT_CONF_FOLDER = "/conf/cf-sanity-test-20191029";
    private static final String TEST_CONTENT_FRAGMENT_PATH = "/content/dam/cf-sanity-test-20191029/en/sample-content-fragment-20191029";
    private static final String TEST_CONTENT_FRAGMENT_MODEL_PATH = "/conf/cf-sanity-test-20191029/settings/dam/cf/models/simple-structure-20191029";
    
    private static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @Rule
    public ContentFragmentRule contentFragmentRule = new ContentFragmentRule(cqBaseClassRule.authorRule);

    private static final InstallPackageRule installPackageRule = new InstallPackageRule(cqBaseClassRule.authorRule, "/test-content", PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static final TestRule ruleChain = RuleChain.outerRule(cqBaseClassRule).around(installPackageRule);

    /**
     * After class in order to make sure that the folders in /content/dam and /conf
     * get properly cleaned up.
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
     * Given a Content Fragment already present on the instance, check to see the availability of the
     * Content Fragment Editor.
     * <p>
     * 1. Check to see if Content Fragment exists on target instance.
     * 2. Open a Content Fragment Editor.
     * 3. Start an editing session in the Content Fragment Editor.
     * 4. Cancel the editing session in the Content Fragment Editor.
     *
     * @throws ClientException - if any operation with the CQClient fails
     * @throws TimeoutException - if the check for Content Fragment existence has reached timeout
     * @throws InterruptedException - if the check for Content Fragment existence has been interrupted
     */
    @Test
    public void testOpenCFEditor() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Testing Content Fragment Editor..");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        new Polling(() -> client.exists(TEST_CONTENT_FRAGMENT_PATH)).poll(TIMEOUT, RETRY_DELAY);

        client.doGet("editor.html/" + TEST_CONTENT_FRAGMENT_PATH, 200);

        LOGGER.info("Testing Content Fragment Editor starting edit..");
        contentFragmentRule.startEdit(TEST_CONTENT_FRAGMENT_PATH);

        LOGGER.info("Testing Content Fragment Editor canceling edit..");
        contentFragmentRule.cancelEdit(TEST_CONTENT_FRAGMENT_PATH);
    }

    /**
     * Given a Content Fragment Model, check to see if the Content Fragment Model Editor is available.
     * <p>
     * 1. Check to see if a Content Fragment Model is available on the instance.
     * 2. Open the Content Fragment Model Editor.
     *
     * @throws ClientException - if any operation with the CQClient fails
     * @throws TimeoutException - if the check for Content Fragment existence has reached timeout
     * @throws InterruptedException - if the check for Content Fragment existence has been interrupted
     */
    @Test
    public void testOpenModelEditor() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Testing Content Fragment Model Editor..");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        new Polling(() -> client.exists(TEST_CONTENT_FRAGMENT_MODEL_PATH)).poll(TIMEOUT, RETRY_DELAY);

        client.doGet("/mnt/overlay/dam/cfm/models/editor/content/editor.html/" + TEST_CONTENT_FRAGMENT_MODEL_PATH, 200);
    }

    /**
     *  Given a Content Fragment Metadata Editor ( Properties screen ) is available.
     * <p>
     *  1. Check to see if a Content Fragment exists.
     *  2. Open the Content Fragment Metadata Editor.
     *
     * @throws ClientException - if any operation with the CQClient fails
     * @throws TimeoutException - if the check for Content Fragment existence has reached timeout
     * @throws InterruptedException - if the check for Content Fragment existence has been interrupted
     */
    @Test
    public void testOpenMetadataEditor() throws ClientException, TimeoutException, InterruptedException {
        LOGGER.info("Testing Open Content Fragment Editor Metadata..");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        new Polling(() -> client.exists(TEST_CONTENT_FRAGMENT_PATH)).poll(TIMEOUT, RETRY_DELAY);

        client.doGet("/mnt/overlay/dam/cfm/admin/content/v2/metadata-editor.html" + TEST_CONTENT_FRAGMENT_PATH, 200);
    }


}
