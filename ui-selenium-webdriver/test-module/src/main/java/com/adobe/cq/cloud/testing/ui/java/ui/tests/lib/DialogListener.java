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

import org.openqa.selenium.*;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

// deals with dialogs that might appear during tests
public class DialogListener implements WebDriverListener {

    public static Logger logger = LoggerFactory.getLogger(DialogListener.class);

    public DialogListener() {

    }

    @Override
    public void beforeAnyWebDriverCall(WebDriver driver, Method method, Object[] args) {
        handleOnBoardingDialog(driver);
    }

    // closes onboarding pop up dialogs that may appear during tests
    private void handleOnBoardingDialog(WebDriver driver){
        try {
            WebElement overlay = driver.findElement(By.cssSelector("coral-overlay[class*='onboarding']"));
            overlay.sendKeys(Keys.ESCAPE);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            logger.debug("No onboarding dialog present");
        }
    }


}
