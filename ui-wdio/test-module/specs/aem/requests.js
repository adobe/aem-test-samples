/*
 *  Copyright 2020 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import { aem } from "../../lib/config.js";
import { expect } from "chai";
import axios from "axios";
import request from "request";

describe("Test requests", () => {
    it.skip("axios should pass", async () => {
        console.log("Checking Google with axios...");
        const axiosResponse = await axios.get(aem.publish.base_url + "?type=axios");
        expect(axiosResponse.status).to.equal(200);
    });

    it.skip("request should pass", async () => {
        console.log("Checking Facebook with request...");
        const requestResponse = await requestAsync(aem.publish.base_url + "?type=request");
        expect(requestResponse.statusCode).to.equal(200);
    });
});

function requestAsync(url) {
    return new Promise((resolve, reject) => {
        request(url, (error, response) => {
            if (error) return reject(error);
            resolve(response);
        });
    });
}
