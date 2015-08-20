package net.sqlcipher.database;

/**
 * Created by user on 8/20/15.
 */
public interface SQLiteStatementBinder {

    /**
     * 绑定参数
     * @param statement
     */
    void bind(SQLiteStatement statement, Object o);

}
