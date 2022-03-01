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

package org.eclipse.jetty.nested.internal;

import java.io.IOException;

import org.eclipse.jetty.nested.NestedConnector;
import org.eclipse.jetty.nested.api.NestedRequestResponse;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NestedChannel extends HttpChannel implements NestedRequestResponse.ReadListener
{
    private static final Logger LOG = LoggerFactory.getLogger(NestedChannel.class);

    private final HttpInput _httpInput;
    private final NestedRequestResponse _nestedRequestResponse;
    private HttpInput.Content _specialContent;

    public NestedChannel(NestedConnector connector, HttpConfiguration configuration, NestedEndpoint endPoint, NestedTransport transport)
    {
        super(connector, configuration, endPoint, transport);
        _httpInput = getRequest().getHttpInput();
        _nestedRequestResponse = endPoint.getNestedRequestResponse();
        _nestedRequestResponse.setReadListener(this);
    }

    @Override
    public boolean needContent()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("needContent()");

        return _nestedRequestResponse.isReadReady();
    }

    @Override
    public HttpInput.Content produceContent()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("produceContent()");

        if (_specialContent != null)
            return _specialContent;

        if (_nestedRequestResponse.isReadClosed())
        {
            _specialContent = new HttpInput.EofContent();
            return _specialContent;
        }

        try
        {
            NestedRequestResponse.Content read = _nestedRequestResponse.read();
            if (read == null)
                return null;

            return new HttpInput.Content(read.getByteBuffer())
            {
                @Override
                public void succeeded()
                {
                    read.release();
                    super.succeeded();
                }

                @Override
                public void failed(Throwable x)
                {
                    read.release();
                    super.failed(x);
                }
            };
        }
        catch (Throwable t)
        {
            _specialContent = new HttpInput.ErrorContent(t);
            return _specialContent;
        }
    }

    @Override
    public boolean failAllContent(Throwable failure)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("failAllContent()", failure);

        // We don't store content so nothing to fail.
        return _nestedRequestResponse.isReadClosed();
    }

    @Override
    public boolean failed(Throwable failure)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("failed()", failure);

        if (_specialContent != null)
            _specialContent = new HttpInput.ErrorContent(failure);
        return _httpInput.onContentProducible();
    }

    @Override
    protected boolean eof()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("eof()");

        try
        {
            _nestedRequestResponse.closeInput();
            if (_specialContent != null)
                _specialContent = new HttpInput.EofContent();
        }
        catch (IOException e)
        {
            if (_specialContent != null)
                _specialContent = new HttpInput.ErrorContent(e);
        }

        return _httpInput.onContentProducible();
    }

    @Override
    public void onDataAvailable()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("onDataAvailable()");

        boolean handle = _httpInput.onContentProducible();
        if (handle)
            execute(this);
    }

    @Override
    public void onAllDataRead()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("onAllDataRead()");

        boolean reschedule = eof();
        if (reschedule)
            execute(this);
    }

    @Override
    public void onError(Throwable t)
    {
        if (LOG.isDebugEnabled())
            LOG.debug("onError()");

        boolean handle = failed(t);
        if (handle)
            execute(this);
    }

    @Override
    public void onCompleted()
    {
        if (LOG.isDebugEnabled())
            LOG.debug("onCompleted()");

        super.onCompleted();
        _nestedRequestResponse.stopAsync();
    }
}