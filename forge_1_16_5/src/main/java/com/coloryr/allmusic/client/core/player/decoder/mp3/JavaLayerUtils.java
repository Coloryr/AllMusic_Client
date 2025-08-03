/*
 * 11/19/04 1.0 moved to LGPL.
 * 12/12/99 Initial version. mdm@techie.com
 * -----------------------------------------------------------------------
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Library General Public License for more details.
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * ----------------------------------------------------------------------
 */

package com.coloryr.allmusic.client.core.player.decoder.mp3;

//import java.io.*;
//import java.lang.reflect.Array;
//
///**
// * The JavaLayerUtils class is not strictly part of the JavaLayer API.
// * It serves to provide useful methods and system-wide hooks.
// *
// * @author MDM
// */
//public class JavaLayerUtils {
//
//    static private final JavaLayerHook hook = null;
//
//    /**
//     * Deserializes an object from the given <code>InputStream</code>.
//     * The deserialization is delegated to an <code>
//     * ObjectInputStream</code> instance.
//     *
//     * @param in The <code>InputStream</code> to deserialize an object
//     *           from.
//     * @return The object deserialized from the stream.
//     * @throws IOException is thrown if there was a problem reading
//     *                     the underlying stream, or an object could not be deserialized
//     *                     from the stream.
//     * @see ObjectInputStream
//     */
//    static public Object deserialize(InputStream in) throws IOException {
//        if (in == null) throw new NullPointerException("in");
//
//        ObjectInputStream objIn = new ObjectInputStream(in);
//
//        Object obj;
//
//        try {
//            obj = objIn.readObject();
//        } catch (ClassNotFoundException ex) {
//            throw new InvalidClassException(ex.toString());
//        }
//
//        return obj;
//    }
//
//    /**
//     * Deserializes an array from a given <code>InputStream</code>.
//     *
//     * @param in       The <code>InputStream</code> to
//     *                 deserialize an object from.
//     * @param elemType The class denoting the type of the array
//     *                 elements.
//     * @param length   The expected length of the array, or -1 if
//     *                 any length is expected.
//     */
//    static public Object deserializeArray(InputStream in, Class elemType, int length) throws IOException {
//        if (elemType == null) throw new NullPointerException("elemType");
//
//        if (length < -1) throw new IllegalArgumentException("length");
//
//        Object obj = deserialize(in);
//
//        Class cls = obj.getClass();
//
//        if (!cls.isArray()) throw new InvalidObjectException("object is not an array");
//
//        Class arrayElemType = cls.getComponentType();
//        if (arrayElemType != elemType) throw new InvalidObjectException("unexpected array component type");
//
//        if (length != -1) {
//            int arrayLength = Array.getLength(obj);
//            if (arrayLength != length) throw new InvalidObjectException("array length mismatch");
//        }
//
//        return obj;
//    }
//
//    static public Object deserializeArrayResource(String name, Class elemType, int length) throws IOException {
//        InputStream str = getResourceAsStream(name); // 这个为null报的错
//        if (str == null) throw new IOException("unable to load resource '" + name + "'");
//
//        return deserializeArray(str, elemType, length);
//    }
//
//    /**
//     * Retrieves an InputStream for a named resource.
//     *
//     * @param name The name of the resource. This must be a simple
//     *             name, and not a qualified package name.
//     * @return The InputStream for the named resource, or null if
//     * the resource has not been found. If a hook has been
//     * provided, its getResourceAsStream() method is called
//     * to retrieve the resource.
//     */
//    static synchronized public InputStream getResourceAsStream(String name) {
//        InputStream is;
//
//        if (hook != null) {
//            is = hook.getResourceAsStream(name);
//        } else {
//            Class<JavaLayerUtils> cls = JavaLayerUtils.class;
//            is = cls.getResourceAsStream(name);
//        }
//
//        return is;
//    }
//}
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.InputStream;

public class JavaLayerUtils
{
    private static JavaLayerHook hook;

    public static Object deserialize(final InputStream in, final Class cls) throws IOException {
        if (cls == null) {
            throw new NullPointerException("cls");
        }
        final Object obj = deserialize(in, cls);
        if (!cls.isInstance(obj)) {
            throw new InvalidObjectException("type of deserialized instance not of required class.");
        }
        return obj;
    }

    public static Object deserialize(final InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }
        final ObjectInputStream objIn = new ObjectInputStream(in);
        Object obj;
        try {
            obj = objIn.readObject();
        }
        catch (ClassNotFoundException ex) {
            throw new InvalidClassException(ex.toString());
        }
        return obj;
    }

    public static Object deserializeArray(final InputStream in, final Class elemType, final int length) throws IOException {
        if (elemType == null) {
            throw new NullPointerException("elemType");
        }
        if (length < -1) {
            throw new IllegalArgumentException("length");
        }
        final Object obj = deserialize(in);
        final Class cls = obj.getClass();
        if (!cls.isArray()) {
            throw new InvalidObjectException("object is not an array");
        }
        final Class arrayElemType = cls.getComponentType();
        if (arrayElemType != elemType) {
            throw new InvalidObjectException("unexpected array component type");
        }
        if (length != -1) {
            final int arrayLength = Array.getLength(obj);
            if (arrayLength != length) {
                throw new InvalidObjectException("array length mismatch");
            }
        }
        return obj;
    }

    public static Object deserializeArrayResource(final String name, final Class elemType, final int length) throws IOException {
        final InputStream str = getResourceAsStream(name);
        if (str == null) {
            throw new IOException("unable to load resource '" + name + "'");
        }
        final Object obj = deserializeArray(str, elemType, length);
        return obj;
    }

    public static void serialize(final OutputStream out, final Object obj) throws IOException {
        if (out == null) {
            throw new NullPointerException("out");
        }
        if (obj == null) {
            throw new NullPointerException("obj");
        }
        final ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(obj);
    }

    public static synchronized void setHook(final JavaLayerHook hook0) {
        JavaLayerUtils.hook = hook0;
    }

    public static synchronized JavaLayerHook getHook() {
        return JavaLayerUtils.hook;
    }

    public static synchronized InputStream getResourceAsStream(final String name) {
        InputStream is = null;
        if (JavaLayerUtils.hook != null) {
            is = JavaLayerUtils.hook.getResourceAsStream(name);
        }
        else {
            final Class cls = JavaLayerUtils.class;
            is = cls.getResourceAsStream(name);
        }
        return is;
    }

    static {
        JavaLayerUtils.hook = null;
    }
}