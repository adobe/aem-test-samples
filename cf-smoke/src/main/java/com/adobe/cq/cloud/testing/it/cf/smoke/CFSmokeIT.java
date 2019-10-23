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

package com.adobe.cq.cloud.testing.it.cf.smoke;

import com.adobe.cq.cloud.testing.it.cf.smoke.rules.ContentFragmentRule;
import com.adobe.cq.cloud.testing.it.cf.smoke.rules.InstallPackageRule;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFSmokeIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFSmokeIT.class);

    private static final String TEST_CONTENT_FRAGMENT_PATH = "/content/dam/cfm-sanity-test/en/sample-structured";
    private static final String TEST_CONTENT_FRAGMENT_MODEL_PATH = "/conf/cfm-sanity-test/settings/dam/cfm/models/simple-structure";

    @ClassRule
    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @Rule
    public ContentFragmentRule contentFragmentRule = new ContentFragmentRule(cqBaseClassRule.authorRule);

    @Test
    public void testCreateContentFragmentWithSimpleModel()  {
        LOGGER.info("Test Create Content Fragment.");




    }

    @Test
    public void testCreateContentFragmentWithComplexModel() {

    }

    @Test
    public void testCreateContentFragmentModel() {

    }

    @Test
    public void testCreateContentFragmentVariation() {

    }


}
