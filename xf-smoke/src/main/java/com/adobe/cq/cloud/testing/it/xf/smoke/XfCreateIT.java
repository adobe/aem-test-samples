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
import com.adobe.cq.testing.client.ExperienceFragmentsClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.usepackage.UsePackageRule;
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

import static com.adobe.cq.testing.client.ExperienceFragmentsClient.*;

/**
 * Test the Create Experience Fragments Operation
 */
public class XfCreateIT {
    private static final Logger LOG = LoggerFactory.getLogger(XfCreateIT.class);

    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.xf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    private static final Logger LOGGER = LoggerFactory.getLogger(XfCreateIT.class);

    public static CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();
    public static InstallPackageRule installPackageRule = new InstallPackageRule(cqAuthorPublishClassRule.authorRule, "/test-content",
            PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule).around(installPackageRule);

    @Rule
    public CQRule cqRule = new CQRule();

    @Rule
    public CleanupRule cleanupRule = new CleanupRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final String DEFAULT_EF_PARENT_PATH = "/content/experience-fragments";
    private static final String TEST_FOLDER = "XfCreateIT-" + UUID.randomUUID();

    /* Values that need to be checked */
    private static final String TEST_DESCRIPTION = "Some description for the test experience fragment";
    private static final String TEST_EF_XF_TITLE = "XfCreateIT Test Experience Fragment";
    private static final String TEST_EF_XF_NAME = "test-xf-name";
    private static final String TEST_EF_VARIANT_TITLE = "XfCreateIT Test Experience Fragment Variant";
    private static final String TEST_EF_VARIANT_NAME ="test-experience-fragment-name";

    @Test
    public void testCreateExperienceFragment() throws ClientException, TimeoutException, InterruptedException {
        createExperienceFragments(DEFAULT_EF_PARENT_PATH);
    }

    @Test
    public void testCreateXFInFolder() throws ClientException, TimeoutException, InterruptedException {
        String folderLocation = cqAuthorPublishClassRule.authorRule.getAdminClient()
                .createFolder(TEST_FOLDER, TEST_FOLDER, DEFAULT_EF_PARENT_PATH)
                .getSlingPath();
        createExperienceFragments(folderLocation);
    }

    private void createExperienceFragments(final String parentPath) throws ClientException, TimeoutException, InterruptedException {

        final ExperienceFragmentsClient xfClient = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);

        LOG.info("Testing {} xf templates", XF_TEMPLATE.values().length);
        for (XF_TEMPLATE predefinedTemplate : XF_TEMPLATE.values()) {
            LOG.info("Testing {}", predefinedTemplate);
            if (predefinedTemplate == XF_TEMPLATE.CUSTOM)
                continue;

            String xfPath = xfClient.experienceFragmentBuilder(TEST_EF_XF_TITLE, TEST_EF_VARIANT_TITLE, predefinedTemplate)
                    .withParentPath(parentPath)
                    .withXFName(TEST_EF_XF_NAME)
                    .withVariantName(TEST_EF_VARIANT_NAME)
                    .withXFDescription(TEST_DESCRIPTION)
                    .create(HttpStatus.SC_CREATED)
                    .getSlingParentLocation();
            Assert.assertTrue("Parent path is incorrect", xfPath.startsWith(parentPath));
            cleanupRule.addPath(xfPath);

            // Wait for experience Fragment to be created
            new Polling(() -> { return xfClient.exists(xfPath); }).poll(TIMEOUT, RETRY_DELAY);

            Assert.assertTrue("Experience Fragment was not created", xfClient.exists(xfPath));
            ExperienceFragment experienceFragment = xfClient.getExperienceFragment(xfPath);
            Assert.assertEquals("Description is incorrect", TEST_DESCRIPTION, experienceFragment.getDescription());
            Assert.assertEquals("Child page is creation failed", experienceFragment.getVariants().size(), 1);
            Assert.assertEquals("XF tags should be empty", experienceFragment.getTags().size(), 0);
            ExperienceFragmentVariant masterVariant = experienceFragment.getVariants().get(0);
            Assert.assertEquals("First variation template is incorrect", masterVariant.getTemplateType(), predefinedTemplate);
            Assert.assertEquals("First variation title is incorrect", TEST_EF_VARIANT_TITLE, masterVariant.getTitle());
            Assert.assertTrue("First variation is not marked as master", masterVariant.isMasterVariant());
            Assert.assertEquals("Variant tags should be empty", masterVariant.getTags().size(), 0);
        }
    }
}
