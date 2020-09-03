package com.adobe.cq.cloud.testing.it.smoke;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.fail;

public class ValidateAntiSamyConfigurationIT {

    private static final String AUTHOR_VALIDATION_URLS = "/com/adobe/cq/cloud/testing/it/smoke/xss/author_validation_urls.json";
    private static final String TEST_REQUEST_PATH = "/libs/cq/xssprotection.json";

    @ClassRule
    public static CQAuthorClassRule cqAuthorClassRule = new CQAuthorClassRule();

    private static CQClient adminAuthor;



    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqAuthorClassRule.authorRule.getAdminClient(CQClient.class);
    }

    @Test
    public void validateConfigurationOnAuthor() throws ClientException, IOException {
        try (InputStream inputStream = this.getClass().getResourceAsStream(AUTHOR_VALIDATION_URLS)) {
            if (inputStream == null) {
                fail("Test failure: unable to read embedded JSON file.");
            }
            HttpEntity httpEntity = new InputStreamEntity(inputStream, ContentType.APPLICATION_JSON);
            SlingHttpResponse response = adminAuthor.doPost(TEST_REQUEST_PATH, httpEntity, 200);
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(response.getContent());
            JsonObject result = jsonElement.getAsJsonObject();
            if (!"ok".equalsIgnoreCase(result.get("status").getAsString())) {
                fail(String.format("Invalid AntiSamy configuration detected. The following URLs were not validated as expected:\n%s",
                        response.getContent()));
            }
        }
    }
}
