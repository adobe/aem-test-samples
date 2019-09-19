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
package com.adobe.cq.cloud.testing.it.xf.smoke.rules;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AllowedTemplatesRule extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger(AllowedTemplatesRule.class);
    private final Instance rule;
    private static final String OOTB_TEMPLATES_PATH = "/libs/settings/experience-fragments(/.*)?";

    /**
     * Allow all ootb templates for the duration of the test enclosed statement
     * @param rule The CQAuthorPublishClassRule
     */
    public AllowedTemplatesRule(Instance rule) {
        this.rule = rule;
    }

    @Override
    protected void before() throws Throwable {
        updateAllowedTemplates(true);
    }

    @Override
    protected void after() {
        try {
            new Polling(() -> {
                updateAllowedTemplates(false);
                return true;
            }).poll(5000, 500);
        } catch (Exception e) {
            LOG.error("Cleanup interrupted", e);
        }
    }

    private void updateAllowedTemplates(boolean allow) throws ClientException {
        String operation = allow ? "+":"-";
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("cq:allowedTemplates", operation + OOTB_TEMPLATES_PATH));
        parameters.add(new BasicNameValuePair("cq:allowedTemplates@TypeHint", "String[]"));
        parameters.add(new BasicNameValuePair("cq:allowedTemplates@Patch", "true"));

        HttpEntity eb = FormEntityBuilder.create().addAllParameters(parameters).build();
        rule.getAdminClient().doPost("/content/experience-fragments", eb, 200);

    }
}
