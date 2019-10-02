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
package com.adobe.cq.cloud.testing.it.xf.smoke;

import com.adobe.cq.cloud.testing.it.xf.smoke.rules.CleanupRule;
import com.adobe.cq.cloud.testing.it.xf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ExperienceFragmentsClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.AUTHOR_PASSWORD;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.AUTHOR_USER_NAME;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_GROUP;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_NAME;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_VERSION;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.RETRY_DELAY;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.TIMEOUT;
import static com.adobe.cq.testing.client.ExperienceFragmentsClient.XF_TEMPLATE;

/**
 * Tests the deletion of variations.
 */
public class VariantDeleteIT {
    private static final String TEST_EF_XF_TITLE = VariantDeleteIT.class.getSimpleName();
    private static final String TEST_EF_VARIANT_TITLE = "Test Experience Fragment Variant";

    private static final String
            TEST_EF_MASTER_VARIANT_TITLE = "Master " + TEST_EF_VARIANT_TITLE;

    private static CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();
    private static InstallPackageRule installPackageRule = new InstallPackageRule(cqAuthorPublishClassRule.authorRule, "/test-content",
            PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static final TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule).around(installPackageRule);

    @Rule
    public CQRule cqRule = new CQRule();

    @Rule
    public CleanupRule cleanupRule = new CleanupRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    @Test
    public void masterVariantDelete() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient authorXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);

        for(XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if(template == XF_TEMPLATE.CUSTOM) continue;

            String variantPath = authorXFClient
                    .createExperienceFragment(TEST_EF_XF_TITLE, TEST_EF_MASTER_VARIANT_TITLE, template)
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
                .createExperienceFragment(TEST_EF_XF_TITLE, TEST_EF_MASTER_VARIANT_TITLE, XF_TEMPLATE.WEB)
                .getSlingParentLocation();

        cleanupRule.addPath(xfPath);

        for(XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM) continue;

            String variantPath = authorXFClient.createXfVariant(xfPath, template, TEST_EF_VARIANT_TITLE)
                                               .getSlingLocation();

            authorXFClient.deleteXfVariant(variantPath, HttpStatus.SC_OK);
            Assert.assertFalse("Variant should be deleted", authorXFClient.exists(variantPath));
        }
    }

}
