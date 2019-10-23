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

import com.adobe.cq.cloud.testing.it.cf.smoke.rules.ContentFragmentRule;
import com.adobe.cq.cloud.testing.it.cf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.sling.testing.clients.ClientException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFEditorSmokeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFEditorSmokeIT.class);

    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.cf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    private static final String TEST_CONTENT_FRAGMENT_PATH = "/content/dam/cfm-sanity-test/en/sample-structured";
    private static final String TEST_CONTENT_FRAGMENT_MODEL_PATH = "/conf/cfm-sanity-test/settings/dam/cfm/models/simple-structure";

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @Rule
    public ContentFragmentRule contentFragmentRule = new ContentFragmentRule(cqBaseClassRule.authorRule);

    @ClassRule
    public static InstallPackageRule installPackageRule = new InstallPackageRule(cqBaseClassRule.authorRule, "/test-content", PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqBaseClassRule).around(installPackageRule);

    @Test
    public void testOpenCFEditor() throws ClientException {
        LOGGER.info("Testing Content Fragment Editor..");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        client.doGet(TEST_CONTENT_FRAGMENT_PATH, 200);

        LOGGER.info("Testing Content Fragment Editor starting edit..");
        contentFragmentRule.startEdit(TEST_CONTENT_FRAGMENT_PATH);

        LOGGER.info("Testing Content Fragment Editor canceling edit..");
        contentFragmentRule.cancelEdit(TEST_CONTENT_FRAGMENT_PATH);
    }

    @Test
    public void testOpenModelEditor() throws ClientException {
        LOGGER.info("Testing Content Fragment Model Editor..");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        client.doGet(TEST_CONTENT_FRAGMENT_MODEL_PATH, 200);
    }

    @Test
    public void testOpenMetadataEditor() throws ClientException {
        LOGGER.info("Testing Open Content Fragment Editor Metadata..");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        client.doGet("/mnt/overlay/dam/cfm/admin/content/v2/metadata-editor.html" + TEST_CONTENT_FRAGMENT_PATH, 200);
    }


}
