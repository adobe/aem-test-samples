/*************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2023 Adobe
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Adobe and its suppliers, if any. The intellectual
 * and technical concepts contained herein are proprietary to Adobe
 * and its suppliers and are protected by all applicable intellectual
 * property laws, including trade secret and copyright laws.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe.
 **************************************************************************/
package com.adobe.cq.cloud.testing.it.smoke;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.junit.rules.CQPublishClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PersistedQueryIT {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final long DELAY = TimeUnit.SECONDS.toMillis(1);

    @ClassRule
    public static final CQPublishClassRule cqBaseClassRule = new CQPublishClassRule();
    static CQClient publishClient;

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.publishRule);

    @BeforeClass
    public static void beforeClass() {
        // todo: switch to anonymous client, once CQ-4355073 is addressed
        publishClient = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
    }

    /**
     * Verifies GET request to persisted query servlet is successful
     *
     * @throws InterruptedException in case if error is occurred
     * @throws TimeoutException     in case if error is occurred
     */
    @Ignore
    @Test
    public void testPersistedQueryEndpointAccessible() throws InterruptedException, TimeoutException {
        new Polling() {
            @Override
            public Boolean call() throws Exception {
                return HttpStatus.SC_NO_CONTENT == publishClient.doGet("/graphql/execute.json").getStatusLine().getStatusCode();
            }
        }.poll(TIMEOUT, DELAY);
    }
}
