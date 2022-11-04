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

/**
 * Exception on publish page check
 */
public class PublishException extends SmokeTestException {
    public static final String PAGE_NOT_AVAILABLE = "PAGE_NOT_AVAILABLE";
    public static final String PAGE_AVAILABLE = "PAGE_AVAILABLE";

    @SuppressWarnings("unused")
    public PublishException(String errorCode, String message) {
        super(errorCode, message);
    }
    
    public PublishException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }

    public static String getPageErrorCode(int expectedStatus) {
        if (expectedStatus == 200) {
            return PAGE_NOT_AVAILABLE;
        }
        return PAGE_AVAILABLE;
    }
}
