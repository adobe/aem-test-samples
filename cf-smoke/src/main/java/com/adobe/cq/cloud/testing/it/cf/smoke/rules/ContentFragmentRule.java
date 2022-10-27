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
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.exceptions.TestingSetupException;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;

import java.util.UUID;

/**
 * A series of Content Fragment helper methods encapsulated in an External Resource Rule.
 */
public class ContentFragmentRule extends ExternalResource {

    private final Instance instance;
    private CQClient client;

    public ContentFragmentRule(Instance instance) {
        this.instance = instance;
    }

    @Override
    protected void before() {
        client = instance.getAdminClient(CQClient.class);
    }

    /**
     * Given a Content Fragment Path, create a variation on that Content Fragment.
     *
     * @param path - the content fragment path to create the variation to
     * @param variation - the name of the variation to be created
     * @param description - the description attached to the variation
     * @throws ClientException - if the operation to create a variation fails
     */
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

    /**
     * Given a Content Fragment Path, start the editing session by doing
     * a HTTP POST request to a Content Fragment Servlet.
     *
     * @param path - the path of the Content Fragment that starts the edit
     * @throws ClientException - if the HTTP POST fails
     */
    public void startEdit(String path) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "start");

        // post to apply edit
        client.doPost(path + ".cfm.edit.json", applyEdit.build(), null, 200);
    }

    /**
     *
     *  Given the path of a Content Fragment, cancel the ongoing edit session
     *  that has been started, by sending a HTTP POST request to a Content
     *  Fragment Servlet.
     *  It does nothing if there is no active edit session started on the
     *  Content Fragment.
     *
     * @param path - the path of the Content Fragment that should cancel the Edit
     * @throws ClientException - if the HTTP POST operation fails
     */
    public void cancelEdit(String path) throws ClientException {
        // prepare new parameters for the apply edit post
        FormEntityBuilder applyEdit = FormEntityBuilder
                .create()
                .addParameter(":operation", "cancel");

        // post to apply edit
        client.doPost(path + ".cfm.edit.json", applyEdit.build(), null, 200);
    }

    /**
     *  Creates a Content Fragment with the specified parameters. It will
     *  always require a parent path ( location where to create the Content
     *  Fragment ) and a model/template path for the Content Fragment ( the
     *  path of a Content Fragment Model ).
     * <p>
     *  It will return the path of the newly created Content Fragment, if
     *  successful, or throw a Client Exception if it was not.
     *
     * @param parentPath - the location where the Content Fragment should be created (required)
     * @param templatePath - the path of the Content Fragment Model for which this CF is based on (required)
     * @param title - the title for this Content Fragment. If left null, it will be the same as the "name" property.
     * @param name - the name for this Content Fragment. If left null, it will be a randomly generated string prefixed with "content-fragment-"
     * @param description - the description of the Content Fragment. If left null, it will be a blank string.
     * @return - the path of the newly Created Content Fragment
     * @throws ClientException - when either parentPath or templatePath are null or invalid or when there was an error with the create request
     */
    public String createContentFragment(String parentPath, String templatePath, String title, String name, String description) throws ClientException {

        if(parentPath == null || parentPath.equals("")) {
            throw new TestingSetupException("Invalid parent path for Content Fragment creation.");
        }
        
        if(templatePath == null || templatePath.equals("")) {
            throw new TestingSetupException("Invalid template path for Content Fragment creation.");
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

        // uses "NOSONAR" because CQRules:CQBP-71 is triggering, but can be ignored for this test case
        final String CREATE_FRAGMENT_PATH = "/libs/dam/cfm/admin/content/v2/createfragment/submit/_jcr_content.html"; //NOSONAR
        SlingHttpResponse response = client.doPost(CREATE_FRAGMENT_PATH, createParams.build(), null, 201);

        return extractContentFragmentPathFromResponse(response.getContent());
    }

    /**
     * Given a parent location, creates a Content Fragment Model in
     * that location, with the title and description provided.
     *
     * @param parentPath - the location to where to create the Content Fragment Model ( required )
     * @param title - the title of the Content Fragment Model, if left null or empty string, a random title will be generated.
     * @param description - the description of the Content Fragment Model, if left null, it will be a blank string
     * @return - the path to the Content Fragment Model
     * @throws ClientException - if the parent path is invalid or if there was an issue with the client operation
     */
    public String createContentFragmentModel(String parentPath, String title, String description) throws ClientException {

        if(parentPath == null || parentPath.equals("")) {
            throw new TestingSetupException("Invalid parent path for Content Fragment Model creation.");
        }

        if(title == null || title.equals("")) {
            title = "content-fragment-model-title-" + UUID.randomUUID();
        }

        if(description == null) {
            description = "";
        }

        // uses "NOSONAR" because CQRules:CQBP-71 is triggering, but can be ignored for this test case
        final String FRAGMENT_PATH = "/libs/settings/dam/cfm/model-types/fragment"; //NOSONAR

        FormEntityBuilder createParams = FormEntityBuilder.create();
        createParams.addParameter("_charset_", "UTF-8");
        createParams.addParameter(":operation", "cfm:createModel");
        createParams.addParameter("_parentPath_", parentPath);
        createParams.addParameter("modelType", FRAGMENT_PATH);
        createParams.addParameter("./jcr:title", title);
        createParams.addParameter("./jcr:description", description);

        SlingHttpResponse response = client.doPost("/mnt/overlay/dam/cfm/models/console/content/createmodelwizard.html/conf/dam/cfm/models/console/content/createmodelwizard/_jcr_content", createParams.build(), null, 201);

        return extractContentFragmentModelPathFromResponse(response.getContent());
    }

    /**
     *  Extracts a content fragment path from a Granite UI HTML response.
     * <p>
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
     * <p>
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
