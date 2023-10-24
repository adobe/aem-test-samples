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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.adobe.cq.cloud.testing.it.smoke.exception.ServiceException;
import com.adobe.cq.testing.client.CQClient;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
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

    protected static final long TIMEOUT = TimeUnit.MINUTES.toMillis(5);

    public static final String SYSTEM_READY = "systemready";

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final Instance instance;
    private final String runmode;
    private final CQClient adminClient;

    public ServiceAccessibleRule(Instance instance) {
        this.instance = instance;
        this.runmode = instance.getConfiguration().getRunmode();
        this.adminClient = instance.getAdminClient(CQClient.class);
    }

    public Statement apply(Statement base, Description description) {
        Polling polling;

        try {
            // Alternatively, rely on setting -Dsling.client.connection.timeout.seconds=10 from the outside
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(10000)
                    .setSocketTimeout(10000)
                    .build();

            // See https://github.com/apache/sling-org-apache-sling-testing-clients#how-can-i-customize-the-underlying-httpclient
            SlingClient.Builder builder = SlingClient.Builder.create(adminClient.getUrl(), null, null);
            HttpClientBuilder httpBuilder = builder.httpClientBuilder();
            httpBuilder.setDefaultRequestConfig(requestConfig);
            SlingClient client = builder.build();

            AtomicInteger counter = new AtomicInteger();
            polling = new Polling(() -> {
                counter.incrementAndGet();
                HttpResponse httpResponse = client.execute(new HttpGet(adminClient.getUrl(SYSTEM_READY)));
                int status = httpResponse.getStatusLine().getStatusCode();
                String response = EntityUtils.toString(httpResponse.getEntity());
                if (status != 200) {
                    String errMsg = String.format("Status Code - %s, response - %s", status, response);
                    if (counter.get() % 20 == 0) {
                        log.warn(errMsg);
                    }
                    throw new IOException(errMsg);
                }
                log.info("Health check for {} passed - {}", runmode.toUpperCase(), response);
                return true;
            });
            polling.poll(TIMEOUT, 2000);
        } catch (TimeoutException | InterruptedException ce) {
            ServiceException serviceException = new ServiceException(runmode.toUpperCase() + SUFFIX, ce.getMessage());
            log.warn("Health check failure", serviceException);
            // TODO throw exceptions once the instance health check URLs GA
            //throw serviceException;

        } catch (ClientException e) {
            throw new RuntimeException(e);
        }
        return base;
    }
}
