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

import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.BrowserLogsDumpRule;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.Config;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.FailureScreenShotRule;
import org.junit.*;
import org.openqa.selenium.By;

public class LoginTestUI extends AEMTestBase {
    /**
     * Using the following rule will create a screenshot in case of tests failure
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
    }

    @Test
    public void checkLoginForm() {
        logger.info("Navigating to root");
        driver.navigate().to(Config.AEM_AUTHOR_URL + "/");
        logger.info("Finding elements in login form");
        driver.findElement(By.cssSelector("#username"));
        driver.findElement(By.cssSelector("#password"));
        driver.findElement(By.cssSelector("form [type=\"submit\"]"));
    }

    @Test
    public void checkSuccessfulLogin() throws Exception {
        driver.navigate().to(Config.AEM_AUTHOR_URL + "/");

        commands.aemLogin(Config.AEM_AUTHOR_USERNAME, Config.AEM_AUTHOR_PASSWORD);

        driver.findElement(By.cssSelector("coral-shell"));
        driver.findElement(By.cssSelector("coral-shell-header"));
        Assert.assertEquals("AEM Start", driver.getTitle());
    }
}
