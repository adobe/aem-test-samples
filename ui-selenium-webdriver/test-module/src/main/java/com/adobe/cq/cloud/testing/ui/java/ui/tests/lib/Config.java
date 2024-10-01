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

public class Config {
    // Selenium
    public static String SELENIUM_BROWSER = System.getProperty("SELENIUM_BROWSER", "chrome");
    public static String SELENIUM_BASE_URL = System.getProperty("SELENIUM_BASE_URL", "http://localhost:4444");

    // AEM Author
    public static String AEM_AUTHOR_URL = System.getProperty("AEM_AUTHOR_URL", "http://localhost:4502");
    public static String AEM_AUTHOR_USERNAME = System.getProperty("AEM_AUTHOR_USERNAME", "admin");
    public static String AEM_AUTHOR_PASSWORD = System.getProperty("AEM_AUTHOR_PASSWORD", "admin");

    // AEM Publish
    public static String AEM_PUBLISH_URL = System.getProperty("AEM_PUBLISH_URL", "http://localhost:4503");
    public static String AEM_PUBLISH_USERNAME = System.getProperty("AEM_PUBLISH_USERNAME", "admin");
    public static String AEM_PUBLISH_PASSWORD = System.getProperty("AEM_PUBLISH_PASSWORD", "admin");

    // Reports
    public static String REPORTS_PATH = System.getProperty("REPORTS_PATH", "/tmp/reports");
    public static String SCREENSHOTS_PATH = REPORTS_PATH + "/screenshots";

    // File uploads
    public static String UPLOAD_URL = System.getProperty("UPLOAD_URL", "");
    public static String SHARED_FOLDER = System.getProperty("SHARED_FOLDER", "");
}
