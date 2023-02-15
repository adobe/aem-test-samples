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
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.util.Date;

public class BrowserLogsDumpRule implements TestRule {

    private WebDriver driver;
    public BrowserLogsDumpRule(WebDriver driver) {
        this.driver = driver;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    // clear logs
                    driver.manage().logs().get(LogType.BROWSER);
                    base.evaluate();
                } finally {
                    dumpBrowserLogs();
                }
            }
        };
    }

    /**
     * Fetches browser logs and prints them in stdout
     */
    public  void dumpBrowserLogs() {
        LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
        if (logEntries.iterator().hasNext()) {
            System.out.println("\n\n***** Browser logs *****");
        }
        for (LogEntry entry : logEntries) {
            System.out.println(new Date(entry.getTimestamp()) + " " + entry.getLevel() + " " + entry.getMessage());
        }
    }
}
