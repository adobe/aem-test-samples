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
import com.adobe.cq.cloud.testing.it.xf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ExperienceFragmentsClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_GROUP;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_NAME;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_VERSION;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.RETRY_DELAY;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.TIMEOUT;
import static com.adobe.cq.testing.client.ExperienceFragmentsClient.XF_TEMPLATE;

/**
 * Test Delete Experience Fragment Operation
 */
public class XfSmokeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(XfSmokeIT.class);

    private static CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();
    private static InstallPackageRule installPackageRule = new InstallPackageRule(cqAuthorPublishClassRule.authorRule, "/test-content",
            PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule).around(installPackageRule);

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
    private static final String CREATE_VARIANT_NAME ="test-experience-fragment-name";

    private static final String DELETE_XF_TITLE = "DeleteXFTest";
    private static final String DELETE_VARIANT_TITLE = "Test Experience Fragment Variant";

    private static final String VARCREA_XF_TITLE = "VariantCreateTest";
    private static final String VARCREA_VARIANT_TITLE = "Test Experience Fragment Variant";
    private static final String VARCREA_VARIANT_NAME ="test-experience-fragment-name";
    private static final String VARCREA_MASTER_VARIANT_TITLE = "Master " + VARCREA_VARIANT_TITLE;

    private static final String VARDEL_XF_TITLE = "VariantDeleteTest";
    private static final String VARDEL_VARIANT_TITLE = "Test Experience Fragment Variant";
    private static final String VARDEL_MASTER_VARIANT_TITLE = "Master " + VARDEL_VARIANT_TITLE;


    @Test
    public void testCreateExperienceFragment() throws ClientException, TimeoutException, InterruptedException {
        createExperienceFragments(CREATE_XF_PARENT_PATH);
    }

    @Test
    public void testCreateXFInFolder() throws ClientException, TimeoutException, InterruptedException {
        String folderLocation = cqAuthorPublishClassRule.authorRule.getAdminClient()
                .createFolder(CREATE_FOLDER, CREATE_FOLDER, CREATE_XF_PARENT_PATH)
                .getSlingPath();
        createExperienceFragments(folderLocation);
    }

    private void createExperienceFragments(final String parentPath) throws ClientException, TimeoutException, InterruptedException {

        final ExperienceFragmentsClient xfClient = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);

        LOGGER.info("Testing {} xf templates", XF_TEMPLATE.values().length);
        for (XF_TEMPLATE predefinedTemplate : XF_TEMPLATE.values()) {
            LOGGER.info("Testing {}", predefinedTemplate);
            if (predefinedTemplate == XF_TEMPLATE.CUSTOM)
                continue;

            String xfPath = xfClient
                    .experienceFragmentBuilder(CREATE_XF_TITLE, CREATE_VARIANT_TITLE, predefinedTemplate)
                    .withParentPath(parentPath)
                    .withXFName(CREATE_XF_NAME)
                    .withVariantName(CREATE_VARIANT_NAME)
                    .withXFDescription(TEST_DESCRIPTION)
                    .create(HttpStatus.SC_CREATED)
                    .getSlingParentLocation();
            Assert.assertTrue("Parent path is incorrect", xfPath.startsWith(parentPath));
            cleanupRule.addPath(xfPath);

            // Wait for experience Fragment to be created
            new Polling(() -> xfClient.exists(xfPath)).poll(TIMEOUT, RETRY_DELAY);

            Assert.assertTrue("Experience Fragment was not created", xfClient.exists(xfPath));
            ExperienceFragmentsClient.ExperienceFragment experienceFragment = xfClient.getExperienceFragment(xfPath);
            Assert.assertEquals("Description is incorrect", TEST_DESCRIPTION, experienceFragment.getDescription());
            Assert.assertEquals("Child page is creation failed", experienceFragment.getVariants().size(), 1);
            Assert.assertEquals("XF tags should be empty", experienceFragment.getTags().size(), 0);
            ExperienceFragmentsClient.ExperienceFragmentVariant masterVariant = experienceFragment.getVariants().get(0);
            Assert.assertEquals("First variation template is incorrect", masterVariant.getTemplateType(), predefinedTemplate);
            Assert.assertEquals("First variation title is incorrect", CREATE_VARIANT_TITLE, masterVariant.getTitle());
            Assert.assertTrue("First variation is not marked as master", masterVariant.isMasterVariant());
            Assert.assertEquals("Variant tags should be empty", masterVariant.getTags().size(), 0);
        }
    }

    @Test
    public void deleteExperienceFragmentTest() throws ClientException {
        final ExperienceFragmentsClient client = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);
        for (XF_TEMPLATE predefinedTemplate : XF_TEMPLATE.values()) {
            if (predefinedTemplate == XF_TEMPLATE.CUSTOM)
                continue;

            String xfLocation = client
                    .createExperienceFragment(DELETE_XF_TITLE, DELETE_VARIANT_TITLE, predefinedTemplate)
                    .getSlingParentLocation();
            cleanupRule.addPath(xfLocation);

            client.deleteExperienceFragment(xfLocation, false, HttpStatus.SC_PRECONDITION_FAILED);
            Assert.assertTrue("Experience Fragment should not be deleted", client.exists(xfLocation));

            client.deleteExperienceFragment(xfLocation, HttpStatus.SC_OK);
            Assert.assertFalse("Experience Fragment should be deleted", client.exists(xfLocation));
        }
    }

    @Test
    public void createXFVariantTest() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient adminXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);
        String xfPath = adminXFClient
                .createExperienceFragment(VARCREA_XF_TITLE, VARCREA_MASTER_VARIANT_TITLE, XF_TEMPLATE.WEB)
                .getSlingParentLocation();
        cleanupRule.addPath(xfPath);

        for(XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM) continue;

            String variantPath = adminXFClient.xfVariantBuilder(xfPath, template, VARCREA_VARIANT_TITLE)
                .withName(VARCREA_VARIANT_NAME)
                .withDescription(TEST_DESCRIPTION)
                .create()
                .getSlingLocation();

            ExperienceFragmentsClient.ExperienceFragmentVariant variant = adminXFClient.getXFVariant(variantPath);

            Assert.assertFalse("Variant should not be master", variant.isMasterVariant());
            Assert.assertFalse("Variant should not be live copy", variant.isLiveCopy());
            Assert.assertEquals("Is social variant", template.isSocialTemplate(), variant.isSocialVariant());
            Assert.assertFalse("Variant should not be a live copy", variant.isLiveCopy());
            Assert.assertEquals("Variant type", template.variantType(), variant.getVariantType());

            Assert.assertEquals("Variant title", VARCREA_VARIANT_TITLE, variant.getTitle());
            Assert.assertEquals("Variant name", VARCREA_VARIANT_NAME, variant.getName());
            Assert.assertEquals("Variant description", TEST_DESCRIPTION, variant.getDescription());
            Assert.assertEquals("Variant template", template, variant.getTemplateType());
            Assert.assertTrue("Variant tags", variant.getTags().size() == 0);

            adminXFClient.deletePage(new String[] { variantPath }, true, false);
        }
    }

    @Test
    public void masterVariantDelete() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient authorXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);

        for(XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if(template == XF_TEMPLATE.CUSTOM) continue;

            String variantPath = authorXFClient
                    .createExperienceFragment(VARDEL_XF_TITLE, VARDEL_MASTER_VARIANT_TITLE, template)
                    .getSlingLocation();

            cleanupRule.addPath(authorXFClient.getParentXFPath(variantPath));

            authorXFClient.deleteXfVariant(variantPath, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            Assert.assertTrue("Master variant should not be deleted", authorXFClient.exists(variantPath));
        }
    }

    @Test
    public void variantDelete() throws ClientException {
        final CQClient authorAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient authorXFClient = authorAuthor.adaptTo(ExperienceFragmentsClient.class);

        String xfPath = authorXFClient
                .createExperienceFragment(VARDEL_XF_TITLE, VARDEL_MASTER_VARIANT_TITLE, XF_TEMPLATE.WEB)
                .getSlingParentLocation();

        cleanupRule.addPath(xfPath);

        for(XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM) continue;

            String variantPath = authorXFClient.createXfVariant(xfPath, template, VARDEL_VARIANT_TITLE)
                                               .getSlingLocation();

            authorXFClient.deleteXfVariant(variantPath, HttpStatus.SC_OK);
            Assert.assertFalse("Variant should be deleted", authorXFClient.exists(variantPath));
        }
    }

}
