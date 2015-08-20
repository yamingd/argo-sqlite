package com.argo.sqlite;

/**
 * Created by user on 8/13/15.
 */
public interface SqliteBlock<T> {

    /**
     *
     * @param engine
     */
    void execute(T engine);

}
