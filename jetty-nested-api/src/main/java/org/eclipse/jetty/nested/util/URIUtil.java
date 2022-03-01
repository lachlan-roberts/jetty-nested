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

package org.eclipse.jetty.nested.util;

public class URIUtil
{
    public static final String SLASH = "/";

    /**
     * Add two Decoded URI path segments.
     * Handles null and empty paths.  Path and query params (eg ?a=b or
     * ;JSESSIONID=xxx) are not handled
     *
     * @param p1 URI path segment (should be decoded)
     * @param p2 URI path segment (should be decoded)
     * @return Legally combined path segments.
     */
    public static String addPaths(String p1, String p2)
    {
        if (p1 == null || p1.length() == 0)
        {
            if (p1 != null && p2 == null)
                return p1;
            return p2;
        }
        if (p2 == null || p2.length() == 0)
            return p1;

        boolean p1EndsWithSlash = p1.endsWith(SLASH);
        boolean p2StartsWithSlash = p2.startsWith(SLASH);

        if (p1EndsWithSlash && p2StartsWithSlash)
        {
            if (p2.length() == 1)
                return p1;
            if (p1.length() == 1)
                return p2;
        }

        StringBuilder buf = new StringBuilder(p1.length() + p2.length() + 2);
        buf.append(p1);

        if (p1.endsWith(SLASH))
        {
            if (p2.startsWith(SLASH))
                buf.setLength(buf.length() - 1);
        }
        else
        {
            if (!p2.startsWith(SLASH))
                buf.append(SLASH);
        }
        buf.append(p2);

        return buf.toString();
    }

    /** Add a path and a query string
     * @param path The path which may already contain contain a query
     * @param query The query string or null if no query to be added
     * @return The path with any non null query added after a '?' or '&amp;' as appropriate.
     */
    public static String addPathQuery(String path, String query)
    {
        if (query == null)
            return path;
        if (path.indexOf('?') >= 0)
            return path + '&' + query;
        return path + '?' + query;
    }
}
