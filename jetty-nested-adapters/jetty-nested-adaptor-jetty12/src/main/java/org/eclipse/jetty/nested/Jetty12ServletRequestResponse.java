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

package org.eclipse.jetty.nested;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.nested.api.NestedRequestResponse;
import org.eclipse.jetty.nested.util.URIUtil;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class Jetty12ServletRequestResponse implements NestedRequestResponse
{
    private static final int BUFFER_SIZE = 1024;

    private final Request _request;
    private final Response _response;
    private final byte[] _outputBuffer = new byte[BUFFER_SIZE];
    private boolean _outClosed = false;
    private final HttpOutput _httpOutput;
    private final HttpInput _httpInput;

    public Jetty12ServletRequestResponse(Request request, Response response)
    {
        _request = request;
        _response = response;
        _httpOutput = new HttpOutput(response);
        _httpInput = new HttpInput(request);
    }

    @Override
    public void startAsync()
    {
        // TODO: we are always async?
    }

    @Override
    public void stopAsync()
    {
        // TODO: we are always async?
        _request.succeeded();
    }

    @Override
    public String getRequestURI()
    {
        return URIUtil.addPathQuery(_request.getPath(), _request.getHttpURI().getQuery());
    }

    @Override
    public String getProtocol()
    {
        return _request.getConnectionMetaData().getProtocol();
    }

    @Override
    public String getMethod()
    {
        return _request.getMethod();
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return Collections.enumeration(_request.getHeaders().stream()
            .map(HttpField::getName)
            .collect(Collectors.toList()));
    }

    @Override
    public Enumeration<String> getHeaders(String headerName)
    {
        return _request.getHeaders().getValues(headerName);
    }

    @Override
    public boolean isSecure()
    {
        return _request.isSecure();
    }

    @Override
    public long getContentLengthLong()
    {
        return _request.getContentLength();
    }

    @Override
    public boolean isReadReady()
    {
        return _httpInput.isReady();
    }

    @Override
    public boolean isReadClosed()
    {
        return _httpInput.isFinished();
    }

    @Override
    public void closeInput() throws IOException
    {
        _httpInput.close();
    }

    @Override
    public Content read() throws IOException
    {
        byte[] content = new byte[BUFFER_SIZE];
        int len = _httpInput.read(content);
        if (len < 0)
            return null;

        ByteBuffer contentBuffer = ByteBuffer.wrap(content, 0, len);
        return new Content()
        {
            @Override
            public ByteBuffer getByteBuffer()
            {
                return contentBuffer;
            }

            @Override
            public void release()
            {
            }
        };
    }

    @Override
    public void setReadListener(ReadListener readListener)
    {
        _httpInput.setReadListener(readListener);
    }

    @Override
    public void setStatus(int status)
    {
        _response.setStatus(status);
    }

    @Override
    public void addHeader(String name, String value)
    {
        _response.addHeader(name, value);
    }

    @Override
    public boolean isWriteReady()
    {
        return _httpOutput.isReady();
    }

    @Override
    public boolean isWriteClosed()
    {
        return _outClosed;
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException
    {
        if (buffer.hasArray())
        {
            byte[] array = buffer.array();
            int offset = buffer.arrayOffset() + buffer.position();
            int length = buffer.remaining();
            _httpOutput.write(array, offset, length);
            buffer.position(buffer.position() + length);
        }
        else
        {
            int len = Math.min(buffer.remaining(), _outputBuffer.length);
            buffer.get(_outputBuffer, 0, len);
            _httpOutput.write(_outputBuffer, 0, len);
        }
    }

    @Override
    public void write(boolean last, NestedCallback callback, ByteBuffer... content)
    {
        _response.write(last, Callback.from(callback::succeeded, callback::failed), content);
    }

    @Override
    public void closeOutput() throws IOException
    {
        // todo
    }

    @Override
    public void setWriteListener(WriteListener writeListener)
    {
        _httpOutput.setWriteListener(writeListener);
    }

    @Override
    public String getRemoteAddr()
    {
        return _request.getRemoteAddr();
    }

    @Override
    public int getRemotePort()
    {
        return _request.getRemotePort();
    }

    @Override
    public String getLocalAddr()
    {
        return _request.getLocalAddr();
    }

    @Override
    public int getLocalPort()
    {
        return _request.getLocalPort();
    }
}
