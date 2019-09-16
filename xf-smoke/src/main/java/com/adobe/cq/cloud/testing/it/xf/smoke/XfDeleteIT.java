package com.adobe.cq.cloud.testing.it.xf.smoke;

import com.adobe.cq.cloud.testing.it.xf.smoke.rules.CleanupRule;
import com.adobe.cq.cloud.testing.it.xf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.client.ExperienceFragmentsClient;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.junit.rules.usepackage.UsePackageRule;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.ClientException;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.cq.testing.client.ExperienceFragmentsClient.XF_TEMPLATE;

/**
 * Test Delete Experience Fragment Operation
 */
public class XfDeleteIT {
    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.xf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    private static final Logger LOGGER = LoggerFactory.getLogger(XfDeleteIT.class);

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
