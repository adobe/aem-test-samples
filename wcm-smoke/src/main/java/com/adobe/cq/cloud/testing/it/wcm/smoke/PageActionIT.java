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
package com.adobe.cq.cloud.testing.it.wcm.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.assertion.CQAssert;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.Page;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.exceptions.TestingIOException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 *  Tests basic CQ Page modifications on author instance:<br>
 * <ul>
 *     <li>Delete</li>
 *     <li>Copy</li>
 *     <li>Move</li>
 * </ul>
 *
 */
public class PageActionIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageActionIT.class);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    @ClassRule
    public static final CQAuthorPublishClassRule cqBaseClassRule = new CQAuthorPublishClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @Rule
    public Page root = new Page(cqBaseClassRule.authorRule);

    /**
     * <ul>
     *     <li>Creates a new page below test root page as user 'admin'.</li>
     *     <li>Verifies that the page got correctly created using {@link CQClient#waitExists(String, long, long)} </li>
     *     <li>Deletes the page as user 'admin'.</li>
     *     <li>Verifies that the page got deleted using {@link CQClient#exists(String)}</li>
     * </ul>
     *
     * @throws ClientException if an error occurred
     * @throws InterruptedException if an error occurred
     * @throws TimeoutException if an error occurred
     */
    @Test
    public void testDeletePageAsAdmin() throws ClientException, InterruptedException, TimeoutException {
        LOGGER.info("Test to check that a page gets deleted properly.");
        final CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        // page title to be set and tested for
        String pageTitle = "QA Test Delete Page " + RandomStringUtils.random(10, true, true);

        // first create a page to be deleted
        LOGGER.info("Creating a page to delete.");
        client.waitExists(root.getPath(), TIMEOUT, RETRY_DELAY);
        String newPath;
        try (SlingHttpResponse response = client.createPage("qatestdeletepage", pageTitle, root.getPath(), root.getTemplatePath())) {
            newPath = response.getSlingPath();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of page creation", e);
        }

        // make sure the page is created with'admin' account
        client.waitExists(newPath, TIMEOUT, RETRY_DELAY);

        LOGGER.info("Deleting the page.");
        // delete the page
        client.deletePageWithRetry(newPath, true, false, TIMEOUT, RETRY_DELAY, HttpStatus.SC_OK);

        LOGGER.info("Checking if the page was deleted correctly.");
        // check if the page was deleted with 'admin' account
        CQAssert.assertPathDoesNotExistWithTimeout(client, newPath, TIMEOUT, RETRY_DELAY);
    }

    /**
     * <ul>
     *     <li>Creates a new page to copy below test root page as user 'admin'.</li>
     *     <li>Verifies that the page got correctly created using {@link CQClient#waitExists(String, long, long)}</li>
     *     <li>Copies the page to the same folder again as user 'admin'.</li>
     *     <li>Checks if page was copied to the right location</li>
     *     <li>Checks if is the copied by is correctly created {@link CQClient#waitExists(String, long, long)}</li>
     *     <li>Checks if the original page is still intact with {@link CQClient#waitExists(String, long, long)}</li>
     * </ul>
     *
     * @throws ClientException if an error occurred
     * @throws InterruptedException if an error occurred
     * @throws TimeoutException if an error occurred
     */
    @Test
    public void testCopyPageToSameFolderAsAdmin() throws ClientException, InterruptedException, TimeoutException {
        LOGGER.info("Test to check that a page gets copied properly.");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        // page title to be set and tested for
        String pageTitle = "QA Test Copy Page " + RandomStringUtils.random(10, true, true);

        // first create a page to copy
        LOGGER.info("Creating a page to copy.");
        String srcName = "qatestcopypage" + RandomStringUtils.random(10, true, true);
        String originalPath;
        try (SlingHttpResponse response = client.createPage(srcName, pageTitle, root.getPath(), root.getTemplatePath())) {
            originalPath = response.getSlingPath();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of page creation", e);
        }

        // make sure the page is created
        client.waitExists(originalPath, TIMEOUT, RETRY_DELAY);

        // now copy the page
        LOGGER.info("Copying the page to the same folder.");
        String destName = "qacopydestinationpage" + RandomStringUtils.random(10, true, true);
        String[] destPaths = client.copyPage(new String[]{originalPath}, destName, null, root.getPath(), "", false).getSlingCopyPaths();

        LOGGER.info("Checking that the destination path is correct.");
        // check for correct dest path
        assertNotEquals(destPaths.length, 0);
        assertEquals("Wrong destination path!", root.getPath() + "/" + destName, destPaths[0]);

        // check for the copied page
        LOGGER.info("Checking that the copy was successful.");
        CQAssert.assertCQPageExistsWithTimeout(client, destPaths[0], TIMEOUT, RETRY_DELAY);

        // check if the original page still exists
        LOGGER.info("Checking that the original page still exists.");
        client.waitExists(originalPath, TIMEOUT, RETRY_DELAY);
        CQAssert.assertCQPageExistsWithTimeout(client, originalPath, TIMEOUT, RETRY_DELAY);
    }

    /**
     * <ul>
     *     <li>Creates a sub page beneath test root page as user admin.</li>
     *     <li>Creates a new page to move below sub page as user 'admin'.</li>
     *     <li>Verifies that the page got correctly created using {@link CQClient#waitExists(String, long, long)}</li>
     *     <li>Moves the test page under the root page.</li>
     *     <li>Checks if page was moved to the right location</li>
     *     <li>Checks if the moved page is correctly created using {@link CQClient#waitExists(String, long, long)}</li>
     *     <li>Checks if the original page is no longer available with {@link CQAssert#assertCQPageExistsWithTimeout}</li>
     * </ul>
     *
     * @throws ClientException if an error occurred
     * @throws InterruptedException if an error occurred
     * @throws TimeoutException if an error occurred
     */
    @Test
    public void testMovePageToSameFolderAsAdmin() throws ClientException, InterruptedException, TimeoutException {
        LOGGER.info("Test to check that a page was moved successfully.");
        CQClient client = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);

        // create a child of the root page to act as our basis for the move
        LOGGER.info("Creating a child page of root to act as our source folder.");
        String subFolderTitle = "QA Test Sub Folder" + RandomStringUtils.random(10, true, true);
        String subFolderName = "qatestsubfolder" + RandomStringUtils.random(10, true, true);
        String subPage;
        try (SlingHttpResponse response = client.createPage(subFolderName, subFolderTitle, root.getPath(),  root.getPath())) {
            subPage = response.getSlingPath();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of page creation", e);
        }

        // make sure sub folder exists
        client.waitExists(subPage, TIMEOUT, RETRY_DELAY);

        // page title to be set and tested for
        LOGGER.info("Creating the test page beneath the folder.");
        String pageTitle = "QA Test Move Page " + RandomStringUtils.random(10, true, true);
        String srcName = "qatestmovepage" + RandomStringUtils.random(10, true, true);
        String originalPath;
        try (SlingHttpResponse response = client.createPage(srcName, pageTitle, subPage, root.getTemplatePath())) {
            originalPath = response.getSlingPath();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of page creation", e);
        }

        // make sure the page is created
        client.waitExists(originalPath, TIMEOUT, RETRY_DELAY);

        // now move the page
        LOGGER.info("Attempting to move the page.");
        String destName = "qamovedestinationpage" + RandomStringUtils.random(10, true, true);
        String[] destPaths = client.movePage(new String[]{originalPath}, destName, null, root.getPath(), "newmovelabel",
                false, true, null).getSlingCopyPaths();

        // check for correct dest path
        LOGGER.info("Checking if the destination path is correct.");
        assertEquals("Wrong destination path!", root.getPath() + "/" + destName, destPaths[0]);

        // check for the moved page
        LOGGER.info("Checking if the page was moved successfully.");
        CQAssert.assertCQPageExistsWithTimeout(client, destPaths[0], TIMEOUT, RETRY_DELAY);

        // check if the sub page still exists
        LOGGER.info("Checking if the folder still exists.");
        CQAssert.assertCQPageExistsWithTimeout(client, destPaths[0], TIMEOUT, RETRY_DELAY);

        LOGGER.info("Checking if the original page " + originalPath + " does not exist anymore.");
        // check if the original page should not exists
        CQAssert.assertPathDoesNotExistWithTimeout(client, originalPath, TIMEOUT, RETRY_DELAY);
    }

}
