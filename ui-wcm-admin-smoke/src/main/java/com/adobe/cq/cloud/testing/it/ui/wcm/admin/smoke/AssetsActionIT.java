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
package com.adobe.cq.cloud.testing.it.ui.wcm.admin.smoke;

import com.adobe.cq.cloud.testing.it.ui.wcm.admin.smoke.rules.CleanUpRule;
import com.adobe.cq.cloud.testing.it.ui.wcm.admin.smoke.rules.TempFolderRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.assertion.CQAssert;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.ResourceUtil;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

/**
 * A set of simple asset tests to verify that asset creating is working.
 */
public class AssetsActionIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetsActionIT.class);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final String ASSET_TO_UPLOAD = "/com/adobe/cq/cloud/testing/it/ui/wcm/admin/smoke/640x480_test_random_image_00aded0f74394cf08773d9d233c16f96.jpg";

    private static CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule);

    @Rule
    public CQRule cqRule = new CQRule();

    @Rule
    public CleanUpRule cleanUpRule = new CleanUpRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    @Rule
    public TempFolderRule damRootFolderRule = new TempFolderRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    /**
     * <ul> <li>Creates a new Asset below /content/dam/qatestrootfolder.</li> <li>Deletes the Asset as user
     * 'admin'.</li> <li>Verifies that Asset has been correctly deleted</li> </ul>
     *
     * @throws InterruptedException
     * @throws ClientException
     * @throws TimeoutException
     */
    @Test
    public void testCreateDeleteAssetAsAdmin() throws ClientException, InterruptedException, TimeoutException {
        CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        String damRootFolder = damRootFolderRule.getTempFolder();

        LOGGER.info("Testing asset upload through Admin Console.");
        String fileName = ASSET_TO_UPLOAD.substring(ASSET_TO_UPLOAD.lastIndexOf('/') + 1);
        String mimeType = "image/jpeg";

        LOGGER.info("Uploading asset..");
        adminAuthor.waitExists(damRootFolder, TIMEOUT, RETRY_DELAY);
        String newFileHandle = adminAuthor.uploadAsset(fileName, ASSET_TO_UPLOAD, mimeType, damRootFolder, 200, 201).getSlingPath();
        cleanUpRule.addPath(newFileHandle);

        new Polling(() -> adminAuthor.exists(newFileHandle)).poll(TIMEOUT, RETRY_DELAY);

        CQAssert.assertAssetExists(adminAuthor, newFileHandle, ResourceUtil.getResourceAsStream(ASSET_TO_UPLOAD), mimeType);
    }

}
