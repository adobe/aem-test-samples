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
package com.adobe.cq.cloud.testing.ui.java.ui.tests.lib;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

import static com.adobe.cq.cloud.testing.ui.java.ui.tests.AEMTestBase.logger;

/**
 * Helper class containing a pre-defined set of functions for UI testing with Adobe Experience Manager.
 */
public class Commands {
    protected static WebDriver driver;

    public Commands(WebDriver driver) {
        this.driver = driver;
    }

    public void forceLogout() {
        driver.navigate().to(Config.AEM_AUTHOR_URL + "/");

        if (driver.getTitle() != "AEM Sign In") {
            logger.info("Need to log out");
            driver.navigate().to(Config.AEM_AUTHOR_URL + "/system/sling/logout.html");
        }

        //Declare and initialise a fluent wait
        FluentWait<WebDriver> wait = new FluentWait<>(driver);
        // Specify the timeout of the wait
        wait.withTimeout(Duration.ofSeconds(10));
        wait.pollingEvery(Duration.ofMillis(250));
        wait.ignoring(NoSuchElementException.class);
        logger.info("Waiting for login dialog");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("form[name=\"login\"]")));
        driver.findElement(By.cssSelector("form[name=\"login\"]"));
    }

    public void aemLogin(String username, String password) {
        if (driver.findElements(By.cssSelector("[class*=\"Accordion\"]")).size() > 0) {
            // Check presence of local sign-in Accordion
            try {
                driver.findElement(By.cssSelector("#username")).click();
                driver.findElement(By.cssSelector("#password")).click();
            }
            // Form field not interactable, not visible
            // Need to open the Accordion
            catch (Exception e) {
                driver.findElement(By.cssSelector("[class*=\"Accordion\"] button")).click();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                }
            }
        }

        driver.findElement(By.cssSelector("#username")).sendKeys(username);
        driver.findElement(By.cssSelector("#password")).sendKeys(password);

        driver.findElement(By.cssSelector("form [type=\"submit\"]")).click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(50));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("coral-shell-content")));
    }


    public void snapshot(String fileName) throws IOException {
        File capture = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");
        String timestamp = dateFormat.format(new Date());

        fileName = Config.SCREENSHOTS_PATH + "/" +  fileName + "-" + timestamp + ".png";
        logger.debug("copying to " + fileName);
        File targetFile = new File(fileName);
        FileUtils.copyFile(capture, targetFile);
    }



}
