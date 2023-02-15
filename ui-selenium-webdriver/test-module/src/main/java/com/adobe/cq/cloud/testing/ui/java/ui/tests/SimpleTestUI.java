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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test case which illustrates Selenium and Java Webdriver interacting
 */
public class SimpleTestUI extends AEMTestBase {
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


    /**
     * Calls static page and verifies the page title on a generic website
     */

    @Test
    public void testPageTitle() {
        logger.info("getting title");
        driver.get(Config.AEM_AUTHOR_URL);
        String pageTitle = "AEM Sign In";
        Assert.assertEquals("Unexpected page title", pageTitle, driver.getTitle());
        // uncomment the line below force failure to demonstrate screenshot in case of failure.
        // Assert.fail();
    }


}
