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
package com.adobe.cq.cloud.testing.it.ui.wcm.admin.smoke.rules;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

/**
 *  <p>Rule that creates temporary resources that can be used for different tests and also
 *  handles the clean up for these resources.</p>
 *
 *  <p>This rule specifically creates a temporary folder under `/content/dam`.</p>
 *
 */
public class TempFolderRule extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempFolderRule.class);

    private static final String CONTENT_DAM_ROOT = "/content/dam";

    private final Instance rule;
    private final long timeout;
    private final long delay;

    private String temporaryFolder;

    /**
     * Creates a temporary folder under /content/dam.
     *
     * @param instance - the CQAuthorPublishClassRule
     * @param timeout - milliseconds timeout for deleting the temporary folder
     * @param delay - delay in milliseconds in between retries to delete the temporary folder
     */
    public TempFolderRule(Instance instance, long timeout, long delay) {
        this.rule = instance;
        this.timeout = timeout;
        this.delay = delay;
    }

    @Override
    protected void before() throws Throwable {
        SlingClient client = rule.getAdminClient();
        String randomName = "test_" + RandomStringUtils.random(10, true, true);
        String randomTitle = "Test " + RandomStringUtils.random(10, true, true);
        temporaryFolder = client.createFolder(randomName, randomTitle, CONTENT_DAM_ROOT, HttpStatus.SC_CREATED).getSlingPath();
    }

    @Override
    protected void after() {
        SlingClient client = rule.getAdminClient();
        try {

            new Polling( () -> {
                client.deletePath(temporaryFolder);
                return !client.exists(temporaryFolder);
            }).poll(timeout, delay);

        } catch(TimeoutException e) {
            LOGGER.error("Timeout occured when trying to delete temporary folder {}", temporaryFolder);
        } catch(InterruptedException e) {
            LOGGER.error("Polling error when trying to delete temporary folder {}", temporaryFolder);
        }
    }


    public String getTempFolder() {
        return temporaryFolder;
    }


}
