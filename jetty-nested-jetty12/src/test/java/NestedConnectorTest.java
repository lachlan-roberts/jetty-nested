//
// ========================================================================
// Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.nested.JettyNestedHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.shaded.deploy.App;
import org.eclipse.jetty.shaded.deploy.DeploymentManager;
import org.eclipse.jetty.shaded.deploy.providers.WebAppProvider;
import org.eclipse.jetty.shaded.server.handler.ContextHandler;
import org.eclipse.jetty.shaded.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.IO;
import org.eclipse.jetty.shaded.webapp.WebAppContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class NestedConnectorTest
{
    private static Server _server;
    private static ServerConnector _connector;
    private static HttpClient _httpClient;
    private static JettyNestedHandler _nestedHandler;

    @BeforeAll
    public static void before() throws Exception
    {
        _server = new Server();
        _connector = new ServerConnector(_server);
        _connector.setPort(8080); // todo remove
        _server.addConnector(_connector);

        // Create a servlet which nests a Jetty server.
        _nestedHandler = new JettyNestedHandler();
        _server.setHandler(_nestedHandler);
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        _nestedHandler.getNestedServer().setHandler(contexts);

        // Add deployment manager to nested server.
        String webapps = Objects.requireNonNull(NestedConnectorTest.class
            .getResource("jetty-base/webapps"))
            .getPath();
        String defaultsDescriptor = Objects.requireNonNull(NestedConnectorTest.class
            .getResource("webdefault.xml"))
            .getPath();
        DeploymentManager deploymentManager = new DeploymentManager();
        deploymentManager.setContexts(contexts);
        WebAppProvider webAppProvider = new WebAppProvider();
        webAppProvider.setExtractWars(true);
        webAppProvider.setDefaultsDescriptor(defaultsDescriptor);
        webAppProvider.setMonitoredDirName(webapps);
        deploymentManager.addAppProvider(webAppProvider);
        _nestedHandler.getNestedServer().addBean(deploymentManager);

        // Start server and client.
        _server.start();
        _httpClient = new HttpClient();
        _httpClient.start();
    }

    @AfterAll
    public static void after() throws Exception
    {
        _httpClient.stop();
        _server.stop();
    }

    public static class TestServlet extends HttpServlet
    {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
        {
            ServletInputStream inputStream = req.getInputStream();
            String requestContent = IO.toString(inputStream);
            PrintWriter writer = resp.getWriter();
            writer.println("we got the request content: ");
            writer.println(requestContent);
            writer.flush();
        }
    }

    @Test
    public void join() throws Exception
    {
        _nestedHandler.getNestedServer().dumpStdErr();
        _server.join();
    }

    private void testResponse() throws Exception
    {
        URL uri = new URL("http://localhost:" + _connector.getLocalPort());
        HttpURLConnection connection = (HttpURLConnection)uri.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        try(PrintWriter writer = new PrintWriter(connection.getOutputStream()))
        {
            writer.println("hello world this is the content 123");
        }

        System.err.println();
        System.err.println(connection.getHeaderField(null));
        connection.getHeaderFields().entrySet()
            .stream()
            .filter(e -> e.getKey() != null)
            .forEach(e -> System.err.printf("  %s: %s\n", e.getKey(), e.getValue()));

        if (connection.getContentLengthLong() != 0)
            System.err.println("\n" + IO.toString(connection.getInputStream()));
        System.err.println();

        assertThat(connection.getResponseCode(), equalTo(200));
    }

    @Test
    public void testPost() throws Exception
    {
        testResponse();
    }
}
