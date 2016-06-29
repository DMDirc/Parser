/*
 * Copyright (c) 2006-2016 DMDirc Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dmdirc.parser.irc.integration.util;

import com.google.common.collect.Iterables;

import org.junit.Assume;
import org.junit.rules.ExternalResource;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * Rule that launchers a container with a given image, and tears it down on completion.
 */
public class DockerContainerRule extends ExternalResource {

    private final String image;
    private DockerClient client;
    private String container;
    private String ip;

    public DockerContainerRule(final String image) {
        this.image = image;
    }

    @Override
    @SuppressWarnings("resource")
    protected void before() throws Throwable {
        super.before();

        client = DockerClientBuilder.getInstance().build();
        client.pullImageCmd(image).exec(new PullImageResultCallback()).awaitSuccess();

        container = client.createContainerCmd(image).exec().getId();
        client.startContainerCmd(container).exec();

        final ContainerNetwork network = Iterables.getFirst(
                client.inspectContainerCmd(container)
                        .exec()
                        .getNetworkSettings()
                        .getNetworks().values(), null);
        Assume.assumeNotNull(network);

        ip = network.getIpAddress();
    }

    @Override
    protected void after() {
        super.after();

        client.stopContainerCmd(container).exec();
        client.removeContainerCmd(container).withForce(true).exec();
    }

    public String getContainerIpAddress() {
        return ip;
    }

}
