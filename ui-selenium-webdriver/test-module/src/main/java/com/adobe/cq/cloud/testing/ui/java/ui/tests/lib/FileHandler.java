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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileHandler {

    private String localAssetpath;
    public FileHandler(String localAssetPath) {
        this.localAssetpath = localAssetPath;
    }

    /** returns a file handle which can be used by selenium for uploading images handles both cases of
     *  a. local testing -> copy to shared location
     *  b. cloud testing -> upload file to location
     */

    public String of(String filename) throws IOException {
        String filePath = "";
        File file = new File(localAssetpath);
        String absolutePath = file.getAbsolutePath();
        File sourceFile = new File(absolutePath,filename);
        if (!"".equals(Config.UPLOAD_URL)) {
            // upload the files to a location where selenium can use them and return the file path
            filePath = fileUploadHandler(sourceFile);
        } else if (!"".equals(Config.SHARED_FOLDER)) {
            // copy the file to SHARED_FOLDER location which is shared via mounts in docker-compose (local testing)
            Path source = sourceFile.toPath();
            Path destination = Paths.get(Config.SHARED_FOLDER + "/" + filename);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            filePath = destination.toString();
        }
        return filePath;
    }

    /** uploads the file to the upload server where it is shared with selenium instance
     * returns an opaque file handle relative to the selenium instance.
     */

    private String fileUploadHandler(File fileToUpload) throws IOException {
        String handle = "";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(Config.UPLOAD_URL);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", fileToUpload, ContentType.DEFAULT_BINARY, fileToUpload.getName());
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            try (CloseableHttpResponse response = httpClient.execute(httpPost)){
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("Received non-200 response from upload server: " + statusCode);
                }
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    handle = EntityUtils.toString(responseEntity);
                }
            }
        }
        return handle;
    }

}
