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

import com.adobe.cq.testing.client.CQAssetsClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

public class AssetsPage {
    private final WebDriver driver;

    private final String ASSETS_PATH = "/content/dam";

    private final SlingClient assetsClient;

    private FileHandler fileHandler;

    public AssetsPage(WebDriver driver, String assetsLocalPath) throws ClientException {
        // decorate with dialog listener which closes dialogs which might interfere with the tests
        WebDriverListener listener = new DialogListener();
        this.driver = new EventFiringDecorator<>(listener).decorate(driver);
        this.assetsClient = new SlingClient(URI.create(Config.AEM_AUTHOR_URL), Config.AEM_AUTHOR_USERNAME, Config.AEM_AUTHOR_PASSWORD);
        this.fileHandler = new FileHandler(assetsLocalPath);
        navigateToAssetsPage();
    }

    private void navigateToAssetsPage(){
        if (!"AEM Assets | Files".equals(driver.getTitle())) {
            this.driver.navigate().to(Config.AEM_AUTHOR_URL + "/assets.html" + ASSETS_PATH);
        }
        this.driver.navigate().refresh();
    }


    public void uploadFile(String filename) throws IOException {
        // get fileHandle relative to selenium
        String fileHandle = this.fileHandler.of(filename);
        WebElement fileUpload =  driver.findElement(By.cssSelector("dam-chunkfileupload > input"));
        fileUpload.sendKeys(fileHandle);
        // wait for 2 seconds for the upload dialog to be interactive
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        WebElement primaryButton = driver.findElement(By.cssSelector("coral-dialog.is-open coral-dialog-footer [variant='primary']"));
        primaryButton.click();
    }


    public void waitForAsset(String file) throws TimeoutException {
        String assetPath = ASSETS_PATH + "/" + file;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(assetExists(assetPath));
    }

    public void waitForAssetDeletion(String file) throws TimeoutException {
        String assetPath = ASSETS_PATH + "/" + file;
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(assetDoesNotExist(Config.AEM_AUTHOR_URL + assetPath));
    }

    public void deleteAsset(String assetFileName) throws ClientException, IOException {
        String assetFilePath = ASSETS_PATH + "/" + assetFileName;
        CQAssetsClient client = this.assetsClient.adaptTo(CQAssetsClient.class);
        client.deletePage(new String[]{assetFilePath}, true, false, 200);
    }



    public  ExpectedCondition<Boolean> assetExists(String url){
        SlingClient slingClient = this.assetsClient;
        return new ExpectedCondition<Boolean>() {

            @Override
            public Boolean apply(WebDriver webDriver) {
                try {
                    CQAssetsClient client = slingClient.adaptTo(CQAssetsClient.class);
                    String status = client.getAssetStatus(url);
                    return true;
                } catch  (ClientException e) {
                    return false;
                }

            }
        };
    }


    public  ExpectedCondition<Boolean> assetDoesNotExist(String url) {
        ExpectedCondition<Boolean> pageExists = this.assetExists(url);
        return webDriver -> !pageExists.apply(webDriver);
    }

}
