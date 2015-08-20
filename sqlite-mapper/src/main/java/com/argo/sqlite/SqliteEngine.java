package com.argo.sqlite;


import android.support.v4.util.ArrayMap;

import java.util.Map;

/**
 * Created by user on 8/13/15.
 */
public class SqliteEngine {
    /**
     *
     */
    static Map<String, SqliteContext> contextMap = new ArrayMap<>();
    /**
     *
     * @param context
     */
    public static void add(SqliteContext context){
        contextMap.put(context.getTag(), context);
    }

    /**
     *
     * @param tag
     * @return
     */
    public static SqliteContext find(String tag){
        return contextMap.get(tag);
    }

}
