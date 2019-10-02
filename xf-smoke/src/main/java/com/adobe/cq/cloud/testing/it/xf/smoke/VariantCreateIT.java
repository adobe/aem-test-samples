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
import static com.adobe.cq.testing.client.ExperienceFragmentsClient.*;

/**
 * Test the creation of variations for different types.
 */
public class VariantCreateIT {
    private static final String TEST_EF_XF_TITLE = VariantCreateIT.class.getSimpleName();

    private static final String TEST_DESCRIPTION = "Some description for the test experience fragment";
    private static final String TEST_EF_VARIANT_TITLE = "Test Experience Fragment Variant";
    private static final String TEST_EF_VARIANT_NAME ="test-experience-fragment-name";

    private static final String TEST_EF_MASTER_VARIANT_TITLE = "Master " + TEST_EF_VARIANT_TITLE;

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
    public void createXFVariantTest() throws ClientException {
        final CQClient adminAuthor = cqAuthorPublishClassRule.authorRule.getAdminClient(CQClient.class);
        final ExperienceFragmentsClient adminXFClient = adminAuthor.adaptTo(ExperienceFragmentsClient.class);
        String xfPath = adminXFClient
                .createExperienceFragment(TEST_EF_XF_TITLE, TEST_EF_MASTER_VARIANT_TITLE, XF_TEMPLATE.WEB)
                .getSlingParentLocation();
        cleanupRule.addPath(xfPath);

        for(XF_TEMPLATE template : XF_TEMPLATE.values()) {
            if (template == XF_TEMPLATE.CUSTOM) continue;

            String variantPath = adminXFClient.xfVariantBuilder(xfPath, template, TEST_EF_VARIANT_TITLE)
                .withName(TEST_EF_VARIANT_NAME)
                .withDescription(TEST_DESCRIPTION)
                .create()
                .getSlingLocation();

            ExperienceFragmentVariant variant = adminXFClient.getXFVariant(variantPath);

            Assert.assertFalse("Variant should not be master", variant.isMasterVariant());
            Assert.assertFalse("Variant should not be live copy", variant.isLiveCopy());
            Assert.assertEquals("Is social variant", template.isSocialTemplate(), variant.isSocialVariant());
            Assert.assertFalse("Variant should not be a live copy", variant.isLiveCopy());
            Assert.assertEquals("Variant type", template.variantType(), variant.getVariantType());

            Assert.assertEquals("Variant title", TEST_EF_VARIANT_TITLE, variant.getTitle());
            Assert.assertEquals("Variant name", TEST_EF_VARIANT_NAME, variant.getName());
            Assert.assertEquals("Variant description", TEST_DESCRIPTION, variant.getDescription());
            Assert.assertEquals("Variant template", template, variant.getTemplateType());
            Assert.assertTrue("Variant tags", variant.getTags().size() == 0);

            adminXFClient.deletePage(new String[] { variantPath }, true, false);
        }
    }

}
