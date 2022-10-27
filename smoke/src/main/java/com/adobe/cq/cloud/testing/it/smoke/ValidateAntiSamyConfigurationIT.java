package com.adobe.cq.cloud.testing.it.smoke;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.poller.Polling;
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
    // uses "NOSONAR" because CQRules:CQBP-71 is triggering, but can be ignored for this test case
    private static final String TEST_REQUEST_PATH = "/libs/cq/xssprotection.json"; //NOSONAR
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final long DELAY = TimeUnit.SECONDS.toMillis(1);

    @ClassRule
    public static final CQAuthorClassRule cqAuthorClassRule = new CQAuthorClassRule();

    private static CQClient adminAuthor;



    @BeforeClass
    public static void beforeClass() {
        adminAuthor = cqAuthorClassRule.authorRule.getAdminClient(CQClient.class);
    }

    @Test
    public void validateConfigurationOnAuthor() throws TimeoutException, InterruptedException {
        new Polling(){
            @Override
            public Boolean call() throws Exception {
                try (InputStream inputStream = this.getClass().getResourceAsStream(AUTHOR_VALIDATION_URLS)) {
                    if (inputStream == null) {
                        fail("Test failure: unable to read embedded JSON file.");
                    }
                    HttpEntity httpEntity = new InputStreamEntity(inputStream, ContentType.APPLICATION_JSON);
                    SlingHttpResponse response = adminAuthor.doPost(TEST_REQUEST_PATH, httpEntity, 200);
                    JsonElement jsonElement = JsonParser.parseString(response.getContent().trim());
                    JsonObject result = jsonElement.getAsJsonObject();
                    if (!"ok".equalsIgnoreCase(result.get("status").getAsString())) {
                        fail(String.format("Invalid AntiSamy configuration detected. The following URLs were not validated as expected:\n%s",
                                response.getContent()));
                    }
                    return true;
                }
            }
        }.poll(TIMEOUT, DELAY);

    }
}
