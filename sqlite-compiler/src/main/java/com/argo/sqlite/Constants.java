package com.argo.sqlite;

import java.util.HashMap;
import java.util.Map;

public class Constants {

    /**
     *
     */
    public static final String MAPPER_CLASS_SUFFIX = "Mapper";

    /**
     * http://www.sqlite.org/datatype3.html
     */
    public static final Map<String, String> JAVA_TO_SQLITE_TYPES;
    static {
        JAVA_TO_SQLITE_TYPES = new HashMap<String, String>();
        JAVA_TO_SQLITE_TYPES.put("byte", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("short", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("int", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("long", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("float", "REAL");
        JAVA_TO_SQLITE_TYPES.put("double", "REAL");
        JAVA_TO_SQLITE_TYPES.put("boolean", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("Byte", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("Short", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("Integer", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("Long", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("Float", "REAL");
        JAVA_TO_SQLITE_TYPES.put("Double", "REAL");
        JAVA_TO_SQLITE_TYPES.put("Boolean", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("String", "TEXT");
        JAVA_TO_SQLITE_TYPES.put("Date", "INTEGER");
        JAVA_TO_SQLITE_TYPES.put("byte[]", "BLOB");
        // TODO: add support for char and Char
    }

    public static final Map<String, String> JAVA_TO_BOX_TYPES;

    static {

        JAVA_TO_BOX_TYPES = new HashMap<String, String>();
        JAVA_TO_BOX_TYPES.put("short", "Short");
        JAVA_TO_BOX_TYPES.put("int", "Integer");
        JAVA_TO_BOX_TYPES.put("long", "Long");
        JAVA_TO_BOX_TYPES.put("float", "Float");
        JAVA_TO_BOX_TYPES.put("double", "Double");
        JAVA_TO_BOX_TYPES.put("boolean", "Boolean");

    }

    public static final Map<String, String> JAVA_TO_BINDING;
    static {
        JAVA_TO_BINDING = new HashMap<String, String>();
        JAVA_TO_BINDING.put("byte", "bindLong");
        JAVA_TO_BINDING.put("short", "bindLong");
        JAVA_TO_BINDING.put("int", "bindLong");
        JAVA_TO_BINDING.put("long", "bindLong");
        JAVA_TO_BINDING.put("float", "bindDouble");
        JAVA_TO_BINDING.put("double", "bindDouble");
        JAVA_TO_BINDING.put("boolean", "bindLong");
        JAVA_TO_BINDING.put("Byte", "bindLong");
        JAVA_TO_BINDING.put("Short", "bindLong");
        JAVA_TO_BINDING.put("Integer", "bindLong");
        JAVA_TO_BINDING.put("Long", "bindLong");
        JAVA_TO_BINDING.put("Float", "bindDouble");
        JAVA_TO_BINDING.put("Double", "bindDouble");
        JAVA_TO_BINDING.put("Boolean", "bindLong");
        JAVA_TO_BINDING.put("String", "bindString");
        JAVA_TO_BINDING.put("Date", "bindLong");
        JAVA_TO_BINDING.put("byte[]", "bindBlob");
        // TODO: add support for char and Char
    }

    public static final Map<String, String> JAVA_TO_SQLITE_GET;
    static {
        JAVA_TO_SQLITE_GET = new HashMap<String, String>();
        JAVA_TO_SQLITE_GET.put("byte", "getShort");
        JAVA_TO_SQLITE_GET.put("short", "getShort");
        JAVA_TO_SQLITE_GET.put("int", "getInt");
        JAVA_TO_SQLITE_GET.put("long", "getLong");
        JAVA_TO_SQLITE_GET.put("float", "getFloat");
        JAVA_TO_SQLITE_GET.put("double", "getDouble");
        JAVA_TO_SQLITE_GET.put("boolean", "getBoolean");
        JAVA_TO_SQLITE_GET.put("Byte", "getShort");
        JAVA_TO_SQLITE_GET.put("Short", "getShort");
        JAVA_TO_SQLITE_GET.put("Integer", "getInt");
        JAVA_TO_SQLITE_GET.put("Long", "getLong");
        JAVA_TO_SQLITE_GET.put("Float", "getFloat");
        JAVA_TO_SQLITE_GET.put("Double", "getDouble");
        JAVA_TO_SQLITE_GET.put("Boolean", "getBoolean");
        JAVA_TO_SQLITE_GET.put("String", "getString");
        JAVA_TO_SQLITE_GET.put("Date", "getDate");
        JAVA_TO_SQLITE_GET.put("byte[]", "getBlob");
        // TODO: add support for char and Char
    }
}
