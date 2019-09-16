package com.adobe.cq.cloud.testing.it.xf.smoke;

import com.adobe.cq.cloud.testing.it.xf.smoke.rules.AllowedTemplatesRule;
import com.adobe.cq.cloud.testing.it.xf.smoke.rules.CleanupRule;
import com.adobe.cq.cloud.testing.it.xf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.ExperienceFragmentsClient;
import com.adobe.cq.testing.client.FoundationClient;
import com.adobe.cq.testing.client.components.foundation.Image;
import com.adobe.cq.testing.client.components.foundation.LayoutContainer;
import com.adobe.cq.testing.client.components.foundation.Text;
import com.adobe.cq.testing.junit.rules.CQAuthorPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.List;

import static com.adobe.cq.testing.client.ExperienceFragmentsClient.*;


/**
 * Test convert to XF operation
 */
public class ConvertToXfIT {
    private static final String PACKAGE_NAME = "com.adobe.cq.cloud.testing.it.xf.smoke";
    private static final String PACKAGE_VERSION = "1.0";
    private static final String PACKAGE_GROUP = "day/cq60/product";

    public static CQAuthorPublishClassRule cqAuthorPublishClassRule = new CQAuthorPublishClassRule();

    public static InstallPackageRule installPackageRule = new InstallPackageRule(cqAuthorPublishClassRule.authorRule, "/test-content",
            PACKAGE_NAME, PACKAGE_VERSION, PACKAGE_GROUP);

    @ClassRule
    public static TestRule ruleChain = RuleChain.outerRule(cqAuthorPublishClassRule).around(installPackageRule);

    public CQRule cqRule = new CQRule();
    public CleanupRule cleanupRule = new CleanupRule(cqAuthorPublishClassRule.authorRule, TIMEOUT, RETRY_DELAY);
    public AllowedTemplatesRule allowedTemplatesRule = new AllowedTemplatesRule(cqAuthorPublishClassRule.authorRule);

    @Rule
    public TestRule rules = RuleChain.outerRule(cqRule).around(cleanupRule).around(allowedTemplatesRule);

    private static final long TIMEOUT = 3000;
    private static final long RETRY_DELAY = 500;

    private static final String TEST_EF_XF_TITLE = ConvertToXfIT.class.getSimpleName();
    private static final String TEST_EF_VARIANT_TITLE = "Test Experience Fragment Variant";
    private static final String TEST_EF_VARIANT_NAME = "test-experience-fragment-variant";

    private static final String TEST_EF_MASTER_VARIANT_TITLE = "Master " + TEST_EF_VARIANT_TITLE;

    private static final String TEST_PAGE_TITLE = "To Convert";
    private static final String TEST_PAGE_NAME = "to-convert";

    private static final String TEXT_COMPONENT_NAME = "text";
    private static final String RESPONSIVE_GRID_NAME = "responsivegrid";
    private static final String IMAGE_COMPONENT_NAME = "image";
    private static final String PAGE_TEXT = "Convert this text";
    private static final String FOLDER_TITLE = "test";
    private static final String PAGE_GRID_PATH = "/jcr:content/root/responsivegrid/";
    private static final String CONVERTABLE_GRID_PATH = PAGE_GRID_PATH + "responsivegrid/";
    private static final String PAGE_TEMPLATE_PATH = "/conf/xf-testing-templates/settings/wcm/templates/xf-testing-template";
    private static final String SITE_PATH = "/content/xf-testing";
    private static final String IMAGE_ASSET_PATH = "/content/dam/xf-testing/cat.jpg";

    private ThreadLocal<String> containerPath = new ThreadLocal<String>() {};

    /**
     * Creates a page with the following structure:
     * - Layout container (The one the template)
     * - Layout container (The one that will converted)
     * - Image
     * - Text
     *
     * @throws Exception
     */
    @Before
    public void before() throws Exception {
        final SlingClient client = cqAuthorPublishClassRule.authorRule.getAdminClient();

        final String pagePath = client.adaptTo(CQClient.class).createPage(TEST_PAGE_NAME, TEST_PAGE_TITLE, SITE_PATH, PAGE_TEMPLATE_PATH)
                .getSlingLocation();
        cleanupRule.addPath(pagePath);

        LayoutContainer layoutContainer = client.adaptTo(FoundationClient.class)
                .addComponent(LayoutContainer.class, pagePath, PAGE_GRID_PATH, RESPONSIVE_GRID_NAME, "last");
        containerPath.set(layoutContainer.getComponentPath());

        Text textComponent = client.adaptTo(FoundationClient.class)
                .addComponent(Text.class, pagePath, CONVERTABLE_GRID_PATH, TEXT_COMPONENT_NAME, "first");
        textComponent.setProperty(Text.PROP_TEXT, PAGE_TEXT);
        textComponent.setProperty(Text.PROP_TEXT_IS_RICH, "true");
        textComponent.save();

        Image imageComponent = client.adaptTo(FoundationClient.class).addComponent(Image.class, pagePath, CONVERTABLE_GRID_PATH, IMAGE_COMPONENT_NAME, "last");
        imageComponent.setImageReference(IMAGE_ASSET_PATH);
        imageComponent.save();
    }

    @Test
    public void testCreateNewXfDefaultParentPath() throws ClientException {
        createNewXF(DEFAULT_XF_PARENT_PATH, null);
    }

    @Test
    public void testCreateNewXfCustomParentPath() throws ClientException {
        String folderPath = cqAuthorPublishClassRule.authorRule.getAdminClient()
                .createFolder(FOLDER_TITLE, FOLDER_TITLE, DEFAULT_XF_PARENT_PATH + "/")
                .getSlingPath();

        cleanupRule.addPath(folderPath);
        createNewXF(folderPath, null);
    }

    private void createNewXF(String parentPath, List<String> tags) throws ClientException {
        final ExperienceFragmentsClient client = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);
        SlingHttpResponse response = client.convertToXF().createNewExperienceFragment(containerPath.get(), parentPath,
                TEST_EF_XF_TITLE, null, TEST_EF_VARIANT_TITLE, XF_TEMPLATE.WEB, null);

        JsonObject responseJson = new Gson().fromJson(response.getContent(), JsonObject.class);
        String variantPath = responseJson.get("variationPath").getAsString();
        String xfPath = getParentXFPath(variantPath);
        cleanupRule.addPath(xfPath);

        ExperienceFragment experienceFragment = client.getExperienceFragment(xfPath);
        Assert.assertTrue("Parent path is incorrect", xfPath.startsWith(parentPath));
        Assert.assertTrue("Experience Fragment was NOT created", client.exists(xfPath));
        Assert.assertEquals("Child page was NOT created", experienceFragment.getVariants().size(), 1);

        ExperienceFragmentVariant masterVariant = experienceFragment.getVariants().get(0);
        Assert.assertEquals("First variation template is incorrect", masterVariant.getTemplateType(), XF_TEMPLATE.WEB);
        Assert.assertEquals("First variation title is incorrect", TEST_EF_VARIANT_TITLE, masterVariant.getTitle());
        Assert.assertTrue("First variation is NOT marked as master", masterVariant.isMasterVariant());
        assertSameImage(client, variantPath);
    }

    @Test
    public void testAddToAnExistingXf() throws ClientException {
        final ExperienceFragmentsClient client = cqAuthorPublishClassRule.authorRule.getAdminClient(ExperienceFragmentsClient.class);
        String xfPath = client.createExperienceFragment(TEST_EF_XF_TITLE, TEST_EF_MASTER_VARIANT_TITLE, XF_TEMPLATE.FACEBOOK)
                .getSlingParentLocation();
        cleanupRule.addPath(xfPath);

        SlingHttpResponse response = client.convertToXF().addToAnExistingXF(
                containerPath.get(), xfPath, TEST_EF_VARIANT_TITLE, XF_TEMPLATE.WEB, null);
        JsonObject responseJson = new Gson().fromJson(response.getContent(), JsonObject.class);
        String variantPath = responseJson.get("variationPath").getAsString();

        ExperienceFragmentVariant variant = client.getXFVariant(variantPath);
        Assert.assertFalse("Variant should not be master", variant.isMasterVariant());
        Assert.assertFalse("Variant should not be live copy", variant.isLiveCopy());
        Assert.assertEquals("Variant should not be social", false, variant.isSocialVariant());
        Assert.assertFalse("Variant should not be a live copy", variant.isLiveCopy());
        Assert.assertEquals("Variant template", XF_TEMPLATE.WEB, variant.getTemplateType());
        Assert.assertEquals("Variant type", XF_TEMPLATE.WEB.variantType(), variant.getVariantType());
        Assert.assertEquals("Variant title", TEST_EF_VARIANT_TITLE, variant.getTitle());
        Assert.assertEquals("Variant name", TEST_EF_VARIANT_NAME, variant.getName());
        assertSameImage(client, variantPath);
    }

    private void assertSameImage(ExperienceFragmentsClient client, String variantPath) throws ClientException {
        ImageComponent imageComponent = new ImageComponent(client, variantPath);
        TextComponent textComponent = new TextComponent(client, variantPath);
        Assert.assertEquals("Image must be the same", IMAGE_ASSET_PATH, imageComponent.getImageReference());
        Assert.assertEquals("Text must be the same", PAGE_TEXT, textComponent.getText());
    }


}
