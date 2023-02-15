/*
 *  Copyright 2022 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.cq.cloud.testing.ui.java.ui.tests;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.AssetsPage;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.BrowserLogsDumpRule;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.Config;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.FailureScreenShotRule;
import org.apache.sling.testing.clients.ClientException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;
import java.io.IOException;

public class AssetUploadTestUI extends AEMTestBase{

    /**
     * Takes a screenshot in case of test failure
     */
    @Rule
    public FailureScreenShotRule failure = new FailureScreenShotRule(driver);
    /**
     * Adds browser logs to the test execution reports for troubleshooting
     */
    @Rule
    public BrowserLogsDumpRule browserLogs =  new BrowserLogsDumpRule(driver);


    @Before
    public void forceLogout() {
        // End any existing user session
        commands.forceLogout();
        // start new one
        commands.aemLogin(Config.AEM_AUTHOR_USERNAME, Config.AEM_AUTHOR_PASSWORD);
    }

    @Test
    public void assetUpload() throws IOException, ClientException {

        String image = "image.png";
        AssetsPage assets = new AssetsPage(driver, "src/main/resources/assets");

        try {
            assets.uploadFile(image);
            commands.snapshot("assetupload");
            assets.waitForAsset(image);
        } catch (TimeoutException e) {
            Assert.fail("Asset " + image + " should be uploaded " + e);
        }

        try {
            assets.deleteAsset(image);
            assets.waitForAssetDeletion(image);
        } catch (TimeoutException | ClientException e) {
            Assert.fail("Asset " + image + " should not exist " + e);
        }
    }

}
