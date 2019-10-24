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

package com.adobe.cq.cloud.testing.it.cf.smoke.rules;

import com.adobe.cq.testing.client.CQClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;

public class ContentFragmentRule extends ExternalResource {

    private Instance instance;
    private CQClient client;

    public ContentFragmentRule(Instance instance) {
        this.instance = instance;
    }

    @Override
    protected void before() {
        client = instance.getAdminClient(CQClient.class);
    }

    @Override
    protected void after() {

    }


    public void updateContentFragmentContent(String path,
                                             String value, String element,
                                             String variation,
                                             boolean newVersion)
            throws ClientException {

        // prepare new parameters for the update post
        FormEntityBuilder requestUpdate = FormEntityBuilder
                .create()
                .addParameter("_charset_", "utf-8")
                .addParameter("contentType", "text/html")
                .addParameter("newVersion", Boolean.toString(newVersion))
                .addParameter("element", element)
                .addParameter("content", value);

        if (StringUtils.isNotBlank(variation)) {
            requestUpdate.addParameter("variation", variation);
        }

        // post to add content fragment asset to the content fragment paragraph
        client.doPost(path + ".cfm.content.json", requestUpdate.build(), null, 200);
    }

    public void updateStructuredContentFragmentContent(String path,
                                                       String value, String element,
                                                       String variation,
                                                       boolean newVersion,
                                                       String contentType)
            throws ClientException {

        // prepare new parameters for the update post
        FormEntityBuilder requestUpdate = FormEntityBuilder
                .create()
                .addParameter(":type", "multiple")
                .addParameter("_charset_", "utf-8")
                .addParameter(":newVersion", Boolean.toString(newVersion))
                .addParameter(element, value)
                .addParameter(element + "@ContentType", contentType);

        if (StringUtils.isNotBlank(variation)) {
            requestUpdate.addParameter(":variation", variation);
        }

        // post to add content fragment asset to the content fragment paragraph
        client.doPost(path + ".cfm.content.json", requestUpdate.build(), null, 200);
    }

    public void createVariation(String path, String variation, String description) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "create")
                .addParameter("_charset_", "utf-8")
                .addParameter("element", "")
                .addParameter("variation", variation)
                .addParameter("description", description);

        // post to apply edit
        client.doPost(path + ".cfm.content.json", applyEdit.build(), null, 200);
    }

    public void removeVariation(String path, String variation) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "remove")
                .addParameter("variation", variation);

        // post to apply edit
        client.doPost(path + ".cfm.content.json", applyEdit.build(), null, 200);
    }

    public void startEdit(String path) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "start");

        // post to apply edit
        client.doPost(path + ".cfm.edit.json", applyEdit.build(), null, 200);
    }

    public void applyEdit(String path) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "apply");

        // post to apply edit
        client.doPost(path + ".cfm.edit.json", applyEdit.build(), null, 200);
    }

    public void cancelEdit(String path) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "cancel");

        // post to apply edit
        client.doPost(path + ".cfm.edit.json", applyEdit.build(), null, 200);
    }

    public String createContentFragment() {
        FormEntityBuilder createParams = FormEntityBuilder.create();



        return "";
    }

}
