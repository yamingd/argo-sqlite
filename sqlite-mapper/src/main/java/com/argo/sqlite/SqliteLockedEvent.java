package com.argo.sqlite;

/**
 * Created by user on 8/31/15.
 */
public class SqliteLockedEvent {

    private String tag;

    public SqliteLockedEvent(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
