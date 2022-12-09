/*
 * Copyright 2018 Adobe Systems Incorporated
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
package com.adobe.cq.cloud.testing.it.xf.smoke;

import com.adobe.cq.cloud.testing.it.xf.smoke.rules.CleanupRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ExperienceFragmentsClient;
import com.adobe.cq.testing.client.ExperienceFragmentsClient.ExperienceFragment;
import com.adobe.cq.testing.client.ExperienceFragmentsClient.ExperienceFragmentVariant;
import com.adobe.cq.testing.client.ExperienceFragmentsClient.XF_TEMPLATE;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;

import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.exceptions.TestingIOException;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.pollinterval.FibonacciPollInterval;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.awaitility.Awaitility.await;

/**
 * Test Delete Experience Fragment Operation
 */
public class XfSmokeIT {

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final Logger LOGGER = LoggerFactory.getLogger(XfSmokeIT.class);

    private static final CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();

    @ClassRule
    public static final TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule);

    @Rule
    public CQRule cqRule = new CQRule();

    @Rule
    public CleanupRule cleanupRule = new CleanupRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    private static final String TEST_DESCRIPTION = "Some description for the test experience fragment";

    private static final String CREATE_XF_PARENT_PATH = "/content/experience-fragments";
    private static final String CREATE_FOLDER = "XfCreateIT-" + UUID.randomUUID();
    private static final String CREATE_XF_TITLE = "XfCreateIT Test Experience Fragment";
    private static final String CREATE_XF_NAME = "test-xf-name";
    private static final String CREATE_VARIANT_TITLE = "XfCreateIT Test Experience Fragment Variant";
    private static final String CREATE_VARIANT_NAME = "test-experience-fragment-name";

    private static final String DELETE_XF_TITLE = "DeleteXFTest";
    private static final String DELETE_VARIANT_TITLE = "Test Experience Fragment Variant";

    private static final String VARCREA_XF_TITLE = "VariantCreateTest";
    private static final String VARCREA_VARIANT_TITLE = "Test Experience Fragment Variant";
    private static final String VARCREA_VARIANT_NAME = "test-experience-fragment-name";
    private static final String VARCREA_MASTER_VARIANT_TITLE = "Master " + VARCREA_VARIANT_TITLE;

    private static final String VARDEL_XF_TITLE = "VariantDeleteTest";
    private static final String VARDEL_VARIANT_TITLE = "Test Experience Fragment Variant";
    private static final String VARDEL_MASTER_VARIANT_TITLE = "Master " + VARDEL_VARIANT_TITLE;

    @BeforeClass
    public static void setupAwait() {
        Awaitility.setDefaultPollInterval(new FibonacciPollInterval(1, TimeUnit.SECONDS));
        Awaitility.setDefaultPollDelay(Durations.TWO_HUNDRED_MILLISECONDS);
        Awaitility.setDefaultTimeout(Durations.ONE_MINUTE);
    }

    @Test
    public void testCreateExperienceFragment() throws ClientException, TimeoutException, InterruptedException {
        createExperienceFragments(CREATE_XF_PARENT_PATH);
    }

    @Test
    public void testCreateXFInFolder() throws ClientException, TimeoutException, InterruptedException {
        String folderLocation;
        try (SlingHttpResponse response = cqAuthorPublishClassRule.authorRule.getAdminClient()
                .createFolder(CREATE_FOLDER, CREATE_FOLDER, CREATE_XF_PARENT_PATH)) {
            folderLocation = response.getSlingPath();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of folder creation", e);
        }
        cleanupRule.addPath(folderLocation);
        createExperienceFragments(folderLocation);
    }

    private void createExperienceFragments(final String parentPath) throws ClientException, TimeoutException, InterruptedException {

        final ExperienceFragmentsClient xfClient = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);

        LOGGER.info("Testing {} xf templates", XF_TEMPLATE.values().length);
        for (XF_TEMPLATE predefinedTemplate : XF_TEMPLATE.values()) {
            LOGGER.info("Testing {}", predefinedTemplate);
            if (predefinedTemplate == XF_TEMPLATE.CUSTOM)
                continue;

            String xfPath;
            try (SlingHttpResponse response = xfClient
                    .experienceFragmentBuilder(CREATE_XF_TITLE, CREATE_VARIANT_TITLE, predefinedTemplate)
                    .withParentPath(parentPath)
                    .withXFName(CREATE_XF_NAME)
                    .withVariantName(CREATE_VARIANT_NAME)
                    .withXFDescription(TEST_DESCRIPTION)
                    .create(HttpStatus.SC_CREATED)) {
                xfPath = response.getSlingParentLocation();
            } catch (IOException e) {
                throw new TestingIOException("Exception while handling sling response (auto-closeable) of fragment creation", e);
            }
            Assert.assertTrue("Parent path is incorrect", xfPath.startsWith(parentPath));
            cleanupRule.addPath(xfPath);

            await().ignoreExceptionsInstanceOf(ClientException.class)
                    .untilAsserted(() -> Assert.assertTrue("Experience Fragment is created", xfClient.exists(xfPath)));
            ExperienceFragment experienceFragment =
                    await().ignoreExceptionsInstanceOf(ClientException.class).until(() -> xfClient.getExperienceFragment(xfPath), Objects::nonNull);
            Assert.assertEquals("Description is incorrect", TEST_DESCRIPTION, experienceFragment.getDescription());
            Assert.assertEquals("Child page is creation failed", experienceFragment.getVariants().size(), 1);
            Assert.assertNotNull("XF tags should not be null", experienceFragment.getTags());
            Assert.assertEquals("XF tags should be empty", experienceFragment.getTags().size(), 0);
            ExperienceFragmentsClient.ExperienceFragmentVariant masterVariant = experienceFragment.getVariants().get(0);
            Assert.assertEquals("First variation template is incorrect", masterVariant.getTemplateType(), predefinedTemplate);
            Assert.assertEquals("First variation title is incorrect", CREATE_VARIANT_TITLE, masterVariant.getTitle());
            Assert.assertTrue("First variation is not marked as master", masterVariant.isMasterVariant());
            Assert.assertNotNull("Variant tags should not be null", masterVariant.getTags());
            Assert.assertEquals("Variant tags should contain at least one tag from the initial content", 1, masterVariant.getTags().size());
        }
    }

    @Test
    public void deleteExperienceFragmentTest() throws ClientException {
        final ExperienceFragmentsClient client = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);
        for (XF_TEMPLATE predefinedTemplate : XF_TEMPLATE.values()) {
            if (predefinedTemplate == XF_TEMPLATE.CUSTOM) {
                continue;
            }

            String xfLocation;
            try (SlingHttpResponse response = client.createExperienceFragment(DELETE_XF_TITLE, DELETE_VARIANT_TITLE, predefinedTemplate)) {
                xfLocation = response.getSlingParentLocation();
            } catch (IOException e) {
                throw new TestingIOException("Exception while handling sling response (auto-closeable) of fragment creation", e);
            }
            cleanupRule.addPath(xfLocation);

            await().untilAsserted(() -> {
                client.deleteExperienceFragment(xfLocation, false, HttpStatus.SC_PRECONDITION_FAILED);
                Assert.assertTrue("Experience Fragment should not be deleted", client.exists(xfLocation));
            });

            await().untilAsserted(() -> {
                client.deleteExperienceFragment(xfLocation, HttpStatus.SC_OK);
                Assert.assertFalse("Experience Fragment should be deleted", client.exists(xfLocation));
            });
        }
    }

    @Test
    public void createXFVariantTest() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient adminXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);
        String xfPath;
        try (SlingHttpResponse response =
                adminXFClient.createExperienceFragment(VARCREA_XF_TITLE, VARCREA_MASTER_VARIANT_TITLE, XF_TEMPLATE.WEB)) {
            xfPath = response.getSlingParentLocation();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of fragment creation", e);
        }
        cleanupRule.addPath(xfPath);

        for (XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM) {
                continue;
            }

            String variantPath;
            try (SlingHttpResponse response = adminXFClient.xfVariantBuilder(xfPath, template, VARCREA_VARIANT_TITLE)
                    .withName(VARCREA_VARIANT_NAME)
                    .withDescription(TEST_DESCRIPTION)
                    .create()) {
                variantPath = response.getSlingLocation();
            } catch (IOException e) {
                throw new TestingIOException("Exception while handling sling response (auto-closeable) of variant creation", e);
            }

            ExperienceFragmentVariant variant =
                    await().ignoreExceptionsInstanceOf(ClientException.class).until(() -> adminXFClient.getXFVariant(variantPath), Objects::nonNull);

            await().ignoreExceptionsInstanceOf(ClientException.class).untilAsserted(() -> {
                Assert.assertFalse("Variant should not be master", variant.isMasterVariant());
                Assert.assertFalse("Variant should not be live copy", variant.isLiveCopy());
                Assert.assertEquals("Is social variant", template.isSocialTemplate(), variant.isSocialVariant());
                Assert.assertFalse("Variant should not be a live copy", variant.isLiveCopy());
                Assert.assertEquals("Variant type", template.variantType(), variant.getVariantType());

                Assert.assertEquals("Variant title", VARCREA_VARIANT_TITLE, variant.getTitle());
                Assert.assertEquals("Variant name", VARCREA_VARIANT_NAME, variant.getName());
                Assert.assertEquals("Variant description", TEST_DESCRIPTION, variant.getDescription());
                Assert.assertEquals("Variant template", template, variant.getTemplateType());
                Assert.assertNotNull("Variant tags should not be null", variant.getTags());
                Assert.assertEquals("Variant tags", 0, variant.getTags().size());
            });

            adminXFClient.deletePage(new String[] {
                    variantPath
            }, true, false);
        }
    }

    @Test
    public void masterVariantDelete() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient authorXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);

        for (XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM) {
                continue;
            }

            String variantPath;
            try (SlingHttpResponse response =
                    authorXFClient.createExperienceFragment(VARDEL_XF_TITLE, VARDEL_MASTER_VARIANT_TITLE, template)) {
                variantPath = response.getSlingLocation();
            } catch (IOException e) {
                throw new TestingIOException("Exception while handling sling response (auto-closeable) of fragment creation", e);
            }

            await().untilAsserted(() -> {
                Assert.assertTrue("Master variant should exist", authorXFClient.exists(variantPath));
            });

            cleanupRule.addPath(ExperienceFragmentsClient.getParentXFPath(variantPath));

            authorXFClient.deleteXfVariant(variantPath, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            await().untilAsserted(() -> {
                Assert.assertTrue("Master variant should not be deleted", authorXFClient.exists(variantPath));
            });
        }
    }

    @Test
    public void variantDelete() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient adminXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);
        String xfPath;
        try (SlingHttpResponse response =
                adminXFClient.createExperienceFragment(VARDEL_XF_TITLE, VARDEL_MASTER_VARIANT_TITLE, XF_TEMPLATE.WEB)) {
            xfPath = response.getSlingParentLocation();
        } catch (IOException e) {
            throw new TestingIOException("Exception while handling sling response (auto-closeable) of fragment creation", e);
        }
        cleanupRule.addPath(xfPath);

        for (XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM)
                continue;

            String variantPath;
            try (SlingHttpResponse response = adminXFClient.createXfVariant(xfPath, template, VARDEL_VARIANT_TITLE)) {
                variantPath = response.getSlingLocation();
            } catch (IOException e) {
                throw new TestingIOException("Exception while handling sling response (auto-closeable) of variant creation", e);
            }

            await().untilAsserted(() -> {
                Assert.assertTrue("Variant should exist", adminXFClient.exists(variantPath));
            });

            adminXFClient.deleteXfVariant(variantPath, HttpStatus.SC_OK);
            await().untilAsserted(() -> {
                Assert.assertFalse("Variant should be deleted", adminXFClient.exists(variantPath));
            });
        }
    }
}
