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

import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.Commands;
import com.adobe.cq.cloud.testing.ui.java.ui.tests.lib.Config;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.logging.Level;


/**
 * This base class will initialize the web driver instance used within the tests.
 */
public abstract class AEMTestBase {
    protected static WebDriver driver;

    protected static Commands commands;

    public static Logger logger = LoggerFactory.getLogger(AEMTestBase.class);

    @BeforeClass
    public static void init() throws Exception {
        // Initialize remote web driver session
        String browser = System.getProperty("SELENIUM_BROWSER", "chrome");
        DesiredCapabilities dc = new DesiredCapabilities();
        // Enable browser logs
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.INFO);

        dc.setBrowserName(browser);
        dc.setPlatform(Platform.LINUX);

        switch (browser) {
            case "chrome":
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--verbose", "--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
                dc.setCapability(ChromeOptions.CAPABILITY, options);
                dc.setCapability("goog:loggingPrefs", logPrefs);
                break;
            case "firefox":
                FirefoxOptions ffOptions = new FirefoxOptions();
                ffOptions.addArguments("-headless");
                dc.setCapability(ChromeOptions.CAPABILITY, ffOptions);
                ffOptions.setLogLevel(FirefoxDriverLogLevel.INFO);
                break;
        }
        URL webDriverUrl = new URL(Config.SELENIUM_BASE_URL + "/wd/hub");
        driver = new RemoteWebDriver(webDriverUrl, dc);
        commands = new Commands(driver);
    }



    @AfterClass
    public static void cleanup() {
        if (driver != null) {
            driver.quit();
        }
    }
}