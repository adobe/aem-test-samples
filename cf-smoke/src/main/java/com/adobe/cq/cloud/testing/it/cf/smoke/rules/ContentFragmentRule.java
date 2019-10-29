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
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ContentFragmentRule extends ExternalResource {

    private static final Logger LOG = LoggerFactory.getLogger(ContentFragmentRule.class);

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

    public String createContentFragment(String parentPath, String templatePath, String title, String name, String description) throws ClientException {

        if(parentPath == null || parentPath.equals("")) {
            LOG.error("Could not create Content Fragment due to invalid parent path.");
            throw new ClientException("Invalid parent path for Content Fragment creation.");
        }
        
        if(templatePath == null || templatePath.equals("")) {
            LOG.error("Could not create Content Fragment due to invalid template path");
            throw new ClientException("Invalid template path for Content Fragment creation.");
        }

        if(name == null || name.equals("")) {
            name = "content-fragment-" + UUID.randomUUID();
        }

        if(title == null || title.equals("")) {
            title = name;
        }

        if(description == null) {
            description = "";
        }

        FormEntityBuilder createParams = FormEntityBuilder.create();
        createParams.addParameter("_charset_", "UTF-8");
        createParams.addParameter("parentPath", parentPath);
        createParams.addParameter("template", templatePath);
        createParams.addParameter("template@Delete", "");
        createParams.addParameter("./jcr:title", title);
        createParams.addParameter("description", description);
        createParams.addParameter("tags@TypeHint", "String[]");
        createParams.addParameter("tags@Delete", "");
        createParams.addParameter("name", name);

        SlingHttpResponse response = client.doPost("/libs/dam/cfm/admin/content/v2/createfragment/submit/_jcr_content.html", createParams.build(), null, 201);

        return extractContentFragmentPathFromResponse(response.getContent());
    }

    public String createContentFragmentModel(String parentPath, String title, String description) throws ClientException {

        if(parentPath == null || parentPath.equals("")) {
            LOG.error("Could not create Content Fragment Model due to invalid parent path.");
            throw new ClientException("Invalid parent path for Content Fragment Model creation.");
        }

        if(title == null || title.equals("")) {
            title = "content-fragment-model-title-" + UUID.randomUUID();
        }

        if(description == null) {
            description = "";
        }

        FormEntityBuilder createParams = FormEntityBuilder.create();
        createParams.addParameter("_charset_", "UTF-8");
        createParams.addParameter(":operation", "cfm:createModel");
        createParams.addParameter("_parentPath_", parentPath);
        createParams.addParameter("modelType", "/libs/settings/dam/cfm/model-types/fragment");
        createParams.addParameter("./jcr:title", title);
        createParams.addParameter("./jcr:description", description);

        SlingHttpResponse response = client.doPost("/mnt/overlay/dam/cfm/models/console/content/createmodelwizard.html/conf/dam/cfm/models/console/content/createmodelwizard/_jcr_content", createParams.build(), null, 201);

        return extractContentFragmentModelPathFromResponse(response.getContent());
    }

    /**
     *  Extracts a content fragment path from a Granite UI HTML response.
     *
     *  As the response returned from a call to create content fragment is a HTML response, we would have to
     *  extract the path from that using String manipulations.
     *
     * @param response - the HTML response we got from creating a content fragment
     * @return - the path of the content fragment.
     */
    private String extractContentFragmentPathFromResponse(String response) {
        // find the index of "Content fragment created successfully at"
        String stringToFind = "<dt class='foundation-form-response-path'>";
        int startIndex = response.indexOf(stringToFind);

        startIndex = startIndex + stringToFind.length();
        String substring = response.substring(startIndex);

        int closingTagIndex = substring.indexOf("</dt>");
        closingTagIndex = closingTagIndex + "</dt>".length() + "<dd>".length() + 1;
        substring = substring.substring(closingTagIndex);

        int lastIndex = substring.indexOf("</dd>");
        return substring.substring(0, lastIndex).trim();
    }

    /**
     *  Extracts a content fragment model path from a Granite UI HTML response.
     *
     *  As the response returned from a call to create content fragment model is a HTML response, we would have to
     *  extract the path from that using String manipulations.
     *
     * @param response - the HTML response we got from creating a content fragment
     * @return - the path of the content fragment.
     */
    private String extractContentFragmentModelPathFromResponse(String response) {

        String marker = "<td><div id=\"Path\">";

        int startIndex = response.indexOf(marker);
        startIndex = startIndex + marker.length();

        String substring = response.substring(startIndex);

        int lastIndex = substring.indexOf("</div>");
        return substring.substring(0, lastIndex).trim();
    }


}
