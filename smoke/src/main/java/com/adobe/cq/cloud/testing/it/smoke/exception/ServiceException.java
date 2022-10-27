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

public class ServiceException extends SmokeTestException {
    public static final String SUFFIX = "_NOT_AVAILABLE";
    
    public ServiceException(String errorCode, String message) {
        super(errorCode, message);
    }

    @SuppressWarnings("unused")
    public ServiceException(String errorCode, String message, Throwable t) {
        super(errorCode, message, t);
    }
}
