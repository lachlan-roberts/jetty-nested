package org.eclipse.jetty.nested;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.nested.api.NestedRequestResponse.ReadListener;
import org.eclipse.jetty.server.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.thread.AutoLock;

public class HttpInput extends InputStream
{
    private final Request _request;
    private Content _content = new Content.Abstract(false, false){};
    private ReadListener _readListener;
    private AtomicBoolean _notifiedReadListener = new AtomicBoolean(false);

    private final AutoLock _lock  = new AutoLock();

    public HttpInput(Request request)
    {
        _request = request;
    }

    public boolean isFinished()
    {
        try(AutoLock l = _lock.lock())
        {
            return _content != null && _content.isLast();
        }
    }

    public boolean isReady()
    {
        try(AutoLock l = _lock.lock())
        {
            if (_content != null)
            {
                if (BufferUtil.hasContent(_content.getByteBuffer()))
                {
                    return true;
                }
                else
                {
                    // Do not demand more content but signal that all data has been read.
                    if (_content.isLast())
                    {
                        // TODO: do outside the lock.
                        if (_readListener != null && !_notifiedReadListener.compareAndSet(false, true))
                        {
                            try
                            {
                                _readListener.onAllDataRead();
                            }
                            catch (Throwable t)
                            {
                                t.printStackTrace();
                            }
                        }
                        return false;
                    }

                    // Release content and demand more.
                    _content.release();
                    _content = null;
                    _request.demandContent(this::onContentAvailable);
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
    }

    public void setReadListener(ReadListener readListener)
    {
        try(AutoLock l = _lock.lock())
        {
            _readListener = readListener;
        }
    }

    private void onContentAvailable()
    {
        try(AutoLock l = _lock.lock())
        {
            _content = _request.readContent();
        }

        if (_readListener != null)
        {
            try
            {
                _readListener.onDataAvailable();
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
    }

    @Override
    public int read() throws IOException
    {
        if (_content == null || !_content.hasRemaining())
            throw new IOException();
        return _content.getByteBuffer().get();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (!isReady())
            return 0;
        return super.read(b, off, len);
    }
}
