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

package com.adobe.cq.cloud.testing.it.smoke.exception;

public class ReplicationException extends SmokeTestException {
    public static final String QUEUE_BLOCKED = "QUEUE_BLOCKED";
    public static final String ACTIVATION_REQUEST_FAILED = "ACTIVATION_REQUEST_FAILED";
    public static final String DEACTIVATION_REQUEST_FAILED = "DEACTIVATION_REQUEST_FAILED";
    public static final String ACTION_NOT_REPLICATED = "ACTION_NOT_REPLICATED";
    public static final String REPLICATION_NOT_AVAILABLE = "REPLICATION_NOT_AVAILABLE";

    @SuppressWarnings("unused")
    public ReplicationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ReplicationException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
