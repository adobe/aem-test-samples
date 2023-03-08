/*************************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2021 Adobe
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
import org.apache.sling.testing.clients.ClientException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class PersistedQueryServletIT {

    @ClassRule
    public static final CQPublishClassRule cqBaseClassRule = new CQPublishClassRule();
    static CQClient adminPublish;

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.publishRule);

    @BeforeClass
    public static void beforeClass() {
        adminPublish = cqBaseClassRule.publishRule.getAdminClient(CQClient.class);
    }

    /**
     * Verifies GET request to persisted query servlet is successful
     *
     * @throws ClientException in case if error is occurred
     */
    @Test
    public void testPersistedQueryListIT() throws ClientException {
        adminPublish.doGet("/graphql/execute.json", 204);
    }
}
