//
//  ========================================================================
//  Copyright (c) 1995-2014 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http2.server;

import java.nio.ByteBuffer;

import org.eclipse.jetty.server.QueuedHttpInput;

public class HttpInputOverHTTP2 extends QueuedHttpInput<ByteBufferCallback>
{
    @Override
    protected int remaining(ByteBufferCallback item)
    {
        return item.getByteBuffer().remaining();
    }

    @Override
    protected int get(ByteBufferCallback item, byte[] buffer, int offset, int length)
    {
        ByteBuffer byteBuffer = item.getByteBuffer();
        length = Math.min(byteBuffer.remaining(), length);
        byteBuffer.get(buffer, offset, length);
        return length;
    }

    @Override
    protected void consume(ByteBufferCallback item, int length)
    {
        ByteBuffer byteBuffer = item.getByteBuffer();
        byteBuffer.position(byteBuffer.position() + length);
        if (!byteBuffer.hasRemaining())
            onContentConsumed(item);
    }

    @Override
    protected void onContentConsumed(ByteBufferCallback item)
    {
        item.succeeded();
    }
}
