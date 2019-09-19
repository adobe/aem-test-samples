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

import com.adobe.cq.testing.client.PackageManagerClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.apache.sling.testing.junit.rules.instance.Instance;
import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.apache.http.HttpStatus.SC_OK;

/**
 * Install the test content package from resources.
 * Takes in an {@code Instance} rule, which has to be applied before this rule
 */
public class InstallPackageRule implements TestRule {
    private static final Logger LOG = LoggerFactory.getLogger(InstallPackageRule.class);

    private String srcPath;
    private final String name;
    private final String version;
    private final String group;

    private Instance instance;

    public  InstallPackageRule(Instance instance, String srcPath, String name, String version, String group) {
        this.instance = instance;
        this.srcPath = srcPath;
        this.name = name;
        this.version = version;
        this.group = group;
    }

    @Override
    public Statement apply(Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AtomicReference<PackageManagerClient.Package> uploadedPackage = new AtomicReference<>(null);
                final PackageManagerClient client = instance.getAdminClient(PackageManagerClient.class);
                final String newPackagePath = buildPath(name, version, group);
                try {
                    // before
                    final File packFile = generatePackage(srcPath);
                    new Polling(() -> {
                        try {
                            uploadedPackage.set(client.uploadPackage(new FileInputStream(packFile), packFile.getName()));
                            uploadedPackage.get().install();
                            Assert.assertEquals("Package path does not match expectations",
                                    newPackagePath, uploadedPackage.get().getPath());
                        } catch (Exception e) {
                            LOG.warn("Package {} was not created.", newPackagePath);
                            cleanupPackage(client, newPackagePath);
                        }
                        return client.isPackageCreated(name, version, group);
                    }).poll(20000, 1000);

                    // statement
                    statement.evaluate();
                } finally {
                    // after
                    if (uploadedPackage.get() != null) {
                        uploadedPackage.get().unInstall();
                        uploadedPackage.get().delete();
                    } else {
                        cleanupPackage(client, newPackagePath);
                    }
                }
            }
        };
    }

    private File generatePackage(String resourceFolder) throws IOException {
        URL res = getClass().getResource(resourceFolder);
        String srcPath = null;
        if (res.toString().startsWith("jar:")) {
            // extract jar in temp folder
            throw new NotImplementedException("Does not support resources in jars");
        } else {
            // resource is not in jar
            srcPath = res.toString();
            if (srcPath.startsWith("file:")) {
                srcPath = srcPath.substring(5);
            }
        }

        return buildJarFromFolder(srcPath);
    }

    private File buildJarFromFolder(String srcPath) throws IOException {
        File generatedPackage = File.createTempFile("temp-package-", ".zip");
        generatedPackage.deleteOnExit();

        Manifest man = new Manifest();

        Attributes atts = man.getMainAttributes();
        atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        atts.putValue("Build-Jdk", ManagementFactory.getRuntimeMXBean().getVmVersion());

        JarOutputStream outJar = new JarOutputStream(new FileOutputStream(generatedPackage), man);

        Iterator<File> fileIter = FileUtils.iterateFilesAndDirs(new File(srcPath), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        while (fileIter.hasNext()) {
            File currFile = fileIter.next();
            if (currFile.isDirectory()) continue;
            String entryName = currFile.getAbsolutePath().substring(srcPath.length()+1);
            JarEntry je = new JarEntry(entryName);
            je.setTime(currFile.lastModified());
            je.setSize(currFile.length());
            outJar.putNextEntry(je);
            IOUtils.copy(new FileInputStream(currFile), outJar);
            outJar.closeEntry();
        }
        outJar.close();
        return generatedPackage;
    }

    // TODO should go into aem-testing-clients
    private SlingHttpResponse cleanupPackage(SlingClient client, String path, String cmd) throws ClientException {
        FormEntityBuilder feb = FormEntityBuilder.create();
        feb.addParameter("cmd", cmd);
        return client.doPost("/crx/packmgr/service/script.html" + path, feb.build(), SC_OK);
    }

    private void cleanupPackage(SlingClient client, String path) throws ClientException {
        cleanupPackage(client, path, "uninstall");
        cleanupPackage(client, path, "delete");
    }

    private String buildPath(String name, String version, String group) {
        if (name == null || "".equals(name)) {
            throw new UnsupportedOperationException("Package name is not set.");
        }
        if (group == null || "".equals(group)) {
            throw new UnsupportedOperationException("Package group is not set.");
        }
        if (version == null || "".equals(version)) {
            return String.format("/etc/packages/%s/%s.zip", group, name);
        }
        return String.format("/etc/packages/%s/%s-%s.zip", group, name, version);
    }



}
