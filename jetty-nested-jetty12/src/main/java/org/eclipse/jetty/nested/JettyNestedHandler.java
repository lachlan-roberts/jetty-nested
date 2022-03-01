package org.eclipse.jetty.nested;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.shaded.server.Server;
import org.eclipse.jetty.shaded.nested.NestedConnector;

public class JettyNestedHandler extends Handler.Abstract
{
    private final Server _server;
    private NestedConnector _connector;

    public JettyNestedHandler()
    {
        _server = new Server();
        _connector = new NestedConnector(_server);
        _server.addConnector(_connector);
    }

    public Server getNestedServer()
    {
        return _server;
    }

    @Override
    protected void doStart() throws Exception
    {
        // Manage LifeCycle manually as eventually this will be shaded and won't implement the same LifeCycle as the Handler.
        _server.start();
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception
    {
        _server.stop();
        super.doStop();
    }

    @Override
    public boolean handle(Request request, Response response) throws Exception
    {
        Jetty12ServletRequestResponse requestResponse = new Jetty12ServletRequestResponse(request, response);
        _connector.service(requestResponse);
        return true;
    }
}
