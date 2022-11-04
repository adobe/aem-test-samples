/*
 * Copyright 2019 Adobe Systems Incorporated
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
package com.adobe.cq.cloud.testing.it.cf.smoke.rules;

import com.adobe.cq.testing.client.PackageManagerClient;
import org.apache.commons.io.IOUtils;
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

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Install the test content package from resources.
 * Takes in an {@code Instance} rule, which has to be applied before this rule
 */
public class InstallPackageRule implements TestRule {
    private static final Logger LOG = LoggerFactory.getLogger(InstallPackageRule.class);

    private final String srcPath;
    private final String name;
    private final String version;
    private final String group;

    private final Instance instance;

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
                PackageManagerClient client = null;
                String newPackagePath = null;

                try {
                    client = instance.getAdminClient(PackageManagerClient.class);
                    newPackagePath = buildPath(name, version, group);
                    // before
                    final File packFile = generatePackage(srcPath);
                    LOG.info("Created package {}", packFile);
                    String finalNewPackagePath = newPackagePath;
                    PackageManagerClient finalClient = client;
                    new Polling(() -> {
                        try {
                            uploadedPackage.set(finalClient.uploadPackage(new FileInputStream(packFile), packFile.getName()));
                            uploadedPackage.get().install();
                            Assert.assertEquals("Package path does not match expectations",
                                    finalNewPackagePath, uploadedPackage.get().getPath());
                        } catch (FileNotFoundException | RuntimeException e) {
                            cleanupPackage(finalClient, finalNewPackagePath);
                        }
                        return finalClient.isPackageCreated(name, version, group);
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

    private File generatePackage(String resourceFolder) throws IOException, URISyntaxException {
        URI resourceUri = Objects.requireNonNull(getClass().getResource(resourceFolder)).toURI();
        Path resourcePath;

        try (FileSystem fs = FileSystems.newFileSystem(resourceUri, Collections.emptyMap())) {
            if (resourceUri.getScheme().equals("jar")) {
                resourcePath = fs.getPath(resourceFolder);
            } else {
                resourcePath = Paths.get(resourceUri);
            }

            LOG.info("Creating package from resources folder {}", resourcePath);
            return buildJarFromFolder(resourcePath);
        }
    }

    private File buildJarFromFolder(Path srcPath) throws IOException {
        File generatedPackage = File.createTempFile("temp-package-", ".zip");
        generatedPackage.deleteOnExit();

        Manifest man = new Manifest();

        Attributes attributes = man.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Build-Jdk", ManagementFactory.getRuntimeMXBean().getVmVersion());

        try (
                JarOutputStream outJar = new JarOutputStream(Files.newOutputStream(generatedPackage.toPath()), man);
                Stream<Path> walk = Files.walk(srcPath, MAX_VALUE)
        ) {
            for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                Path path = it.next();
                if (Files.isDirectory(path)) continue;
                String entryName = path.toString().substring(srcPath.toString().length() + 1);
                JarEntry je = new JarEntry(entryName);
                je.setTime(Files.getLastModifiedTime(path).toMillis());
                je.setSize(Files.size(path));
                outJar.putNextEntry(je);
                IOUtils.copy(Files.newInputStream(path), outJar);
                outJar.closeEntry();
            }
        }

        return generatedPackage;
    }

    // TODO move to aem-testing-clients
    @SuppressWarnings("UnusedReturnValue")
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