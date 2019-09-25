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
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_GROUP;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_NAME;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.PACKAGE_VERSION;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.RETRY_DELAY;
import static com.adobe.cq.cloud.testing.it.xf.smoke.Constants.TIMEOUT;
import static com.adobe.cq.testing.client.ExperienceFragmentsClient.XF_TEMPLATE;

/**
 * Test Delete Experience Fragment Operation
 */
public class XfDeleteIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(XfDeleteIT.class);

    private static CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();
    private static InstallPackageRule installPackageRule = new InstallPackageRule(cqAuthorPublishClassRule.authorRule, "/test-content",
            PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule).around(installPackageRule);

    @Rule
    public CQRule cqRule = new CQRule();

    @Rule
    public CleanupRule cleanupRule = new CleanupRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);

    private static final String TEST_EF_XF_TITLE = XfDeleteIT.class.getSimpleName();
    private static final String TEST_EF_VARIANT_TITLE = "Test Experience Fragment Variant";

    @Test
    public void deleteExperienceFragmentTest() throws ClientException {
        final ExperienceFragmentsClient client = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);
        for (XF_TEMPLATE predefinedTemplate : XF_TEMPLATE.values()) {
            if (predefinedTemplate == XF_TEMPLATE.CUSTOM)
                continue;

            String xfLocation = client
                    .createExperienceFragment(TEST_EF_XF_TITLE, TEST_EF_VARIANT_TITLE, predefinedTemplate)
                    .getSlingParentLocation();
            cleanupRule.addPath(xfLocation);

            client.deleteExperienceFragment(xfLocation, false, HttpStatus.SC_PRECONDITION_FAILED);
            Assert.assertTrue("Experience Fragment should not be deleted", client.exists(xfLocation));

            client.deleteExperienceFragment(xfLocation, HttpStatus.SC_OK);
            Assert.assertFalse("Experience Fragment should be deleted", client.exists(xfLocation));
        }
    }

}
