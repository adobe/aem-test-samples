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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FailureScreenShotRule implements TestRule {
    private WebDriver driver;
    protected static Commands commands;

    public FailureScreenShotRule(WebDriver driver){
        this.driver = driver;
        this.commands = new Commands(driver);
    }

    public static Logger logger = LoggerFactory.getLogger(FailureScreenShotRule.class);


    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    statement.evaluate();
                } catch (Throwable t) {
                    try  {
                        String snapshotName = description.getMethodName();
                        commands.snapshot(snapshotName);
                    } catch (Exception e) {
                        logger.error("could not take snapshot " + e);
                    }
                    throw t;
                }
            }
        };
    }
}
