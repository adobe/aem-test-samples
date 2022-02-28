/*
 * Copyright 2022 Adobe Systems Incorporated
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

package com.adobe.cq.cloud.testing.it.smoke.rules;

import java.io.IOException;

import com.adobe.cq.cloud.testing.it.smoke.exception.ServiceException;
import com.adobe.cq.testing.client.CQClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.cq.cloud.testing.it.smoke.exception.ServiceException.SUFFIX;

/**
 * Junit test rule to check service up
 */
public class ServiceAccessibleRule implements TestRule {
    private static final Logger log = LoggerFactory.getLogger(ServiceAccessibleRule.class);
    
    public static final String SYSTEM_READY = "systemready";
    
    private final Instance instance;
    private final String runmode;
    private final CQClient adminClient;

    public ServiceAccessibleRule(Instance instance) {
        this.instance = instance;
        this.runmode = instance.getConfiguration().getRunmode();
        this.adminClient = instance.getAdminClient(CQClient.class);
    }

    public Statement apply(Statement base, Description description) {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse httpResponse = client.execute(new HttpGet(adminClient.getUrl(SYSTEM_READY)));
            int status = httpResponse.getStatusLine().getStatusCode();
            String response = EntityUtils.toString(httpResponse.getEntity());
            if (status != 200) {
                throw new IOException(String.format("Status Code - %s, response - %s", status, response));
            }
            log.info("Health check for {} passed - {}", runmode.toUpperCase(), response);
        } catch (Exception ce) {
            ServiceException serviceException = new ServiceException(runmode.toUpperCase() + SUFFIX, ce.getMessage());
            log.info("Health check failure", serviceException);
            // TODO throw exceptions once the instance health check URLs GA
            //throw serviceException;
        }
        return base;
    }
}
