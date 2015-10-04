package com.argo.sqlite;

import android.content.Context;
import android.support.v4.util.ArrayMap;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SQLiteTransactionListener;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by user on 8/13/15.
 */
public class SqliteContext {

    public static final String SQLITE_TAG_DEFAULT = "default";
    public static final String SUFFIX = ".db";
    public static final String DATABASE_IS_LOCKED = "database is locked";

    private Context context;

    private File path;
    private String tag;
    private String name;
    private String userId;
    private String originalName;
    private byte[] salt;

    private SQLiteDatabase database;

    public SqliteContext(Context context, byte[] salt) {
        File file = context.getDatabasePath("dump");
        this.context = context;
        this.tag = SQLITE_TAG_DEFAULT;
        this.name = null;
        this.originalName = name;
        this.salt = salt;
        this.path = file.getParentFile();
    }

    public SqliteContext(Context context, String name, byte[] salt) {
        File file = context.getDatabasePath("dump");
        this.context = context;
        this.tag = SQLITE_TAG_DEFAULT;
        this.name = name;
        this.originalName = name;
        this.salt = salt;
        this.path = file.getParentFile();
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        name = "user_" + this.getUserId();
    }

    public SQLiteDatabase getDatabase() {
        if (database == null){
            ensureDbOpen();
        }
        return database;
    }

    public File getDbFolder(String name){
        if (name == null){
            return null;
        }

        File path = new File(this.path, name + SUFFIX);
        //Timber.d("getDbFolder: %s", path);

        try {
            if (path.exists()){
                return path;
            }
        } finally {

        }
        File folder = path.getParentFile();
        if (!folder.exists()){
            folder.mkdirs();
            Timber.d("Create getDbFolder: %s", path);
        }
        return path;
    }

    /**
     *
     * @return
     */
    public boolean exists(){
        if (name != null){
            File file = getDbFolder(name);
            return file.exists();
        }else{
            return true;
        }
    }

    /**
     *
     * @param name
     */
    public void open(String name){
        File path = getDbFolder(name);
        if (database == null) {
            char[] secret = getChars(this.salt);
            database = SQLiteDatabase.openOrCreateDatabase(path.getAbsolutePath(), secret, null);
            database.setLockingEnabled(true);
            Timber.d("open db version: %s(%s)", path, database.getVersion());
        }
    }

    private char[] getChars(byte[] bytes) {
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(bytes.length);
        bb.put(bytes);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

    public void ensureDbOpen(){

        if (database != null){
            return;
        }

        if (null == name){
            name = "user_" + this.getUserId();
        }

        open(name);
    }

    private Map<String, Map<String, Boolean>> tables = null;
    /**
     * 列出表结构和列
     * @return
     */
    public synchronized Map<String, Map<String, Boolean>> getTables(){
        if (null != tables){
            return tables;
        }

        ensureDbOpen();
        tables = new ArrayMap<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name";
        Cursor cursor = this.database.rawQuery(sql, null);
        while (cursor.moveToNext()){
            String name = cursor.getString(0);
            tables.put(name, new ArrayMap<String, Boolean>());
        }
        cursor.close();
        final String format = "PRAGMA table_info('%s')";
        Set<Map.Entry<String, Map<String, Boolean>>> sets = tables.entrySet();
        for (Map.Entry<String, Map<String, Boolean>> item : sets){
            String name = item.getKey();
            sql = String.format(format, name);
            cursor = this.database.rawQuery(sql, null);
            while (cursor.moveToNext()){
                item.getValue().put(cursor.getString(1), true);
            }
            cursor.close();
        }
        return tables;
    }

    /**
     * 读取某个表的字段
     * @param table
     * @return
     */
    public Map<String, Boolean> getTableColumns(String table){
        if (null == tables){
            getTables();
        }
        return tables.get(table);
    }

    /**
     *
     */
    public void clearTables(){
        if (tables == null){
            return;
        }

        tables.clear();
        tables = null;
    }

    /**
     * 新建表
     * @param sql
     */
    public synchronized void createTable(String sql){
        ensureDbOpen();
        this.database.rawExecSQL(sql);
        int v = this.database.getVersion();
        this.database.setVersion(v + 1);
    }

    /**
     * 初始化表
     * @param mapper
     */
    public synchronized void initTable(SqliteMapper mapper){
        String tableName = mapper.getTableName();
        final Map<String, Boolean> columns = this.getTableColumns(tableName);
        if (columns == null || columns.size() == 0) {
            String sql = mapper.getTableCreateSql();
            this.createTable(sql);
        }else{
            this.alterTable(tableName, mapper.getColumnInfo(), columns.keySet());
        }
    }

    /**
     * 更新表
     * @param columns
     */
    public synchronized void alterTable(String tableName, Map<String, String> columns, Set<String> oldColumns){
        ensureDbOpen();
        final String sql = "alter table %s add column %s %s";
        for (String name : oldColumns){
            columns.remove(name);
        }
        Set<Map.Entry<String, String>> itors = columns.entrySet();
        if (itors.size() == 0){
            return;
        }

        for (Map.Entry<String, String> itor : itors){
            this.database.rawExecSQL(String.format(sql, tableName, itor.getKey(), itor.getValue()));
        }
        int v = this.database.getVersion();
        this.database.setVersion(v + 1);
    }

    /**
     *
     */
    public void deleteFile(){
        File file = getDbFolder(this.name);
        if (file != null && file.exists()){
            file.delete();
        }
    }


    /**
     * 更新时使用事务管理
     * @param block
     */
    public synchronized void update(SqliteBlock<SQLiteDatabase> block){
        final long ts = System.currentTimeMillis();
        ensureDbOpen();
        boolean error = false;
        try {
            executeBlock(block);
        }catch (SQLiteException e){
            //net.sqlcipher.database.SQLiteException: error code 5: database is locked
            if (e.getMessage().contains(DATABASE_IS_LOCKED)){
                this.database.endTransaction();

                foreceCloseWhenLocked();

                try {
                    executeBlock(block);
                } catch (Exception e1) {
                    error = true;
                    Timber.e(e, "update Error. db-%s", getTag());
                }
            }
        }
        catch (Exception e) {
            error = true;
            Timber.e(e, "update Error. db-%s", getTag());
        }finally {
            if (!error){
                this.database.setTransactionSuccessful();
            }
            this.database.endTransaction();
            long ts0 = System.currentTimeMillis() - ts;
            Timber.i("db-%s update complete duration: %s ms", getTag(), ts0);
        }

    }

    private synchronized void foreceCloseWhenLocked(){
        Timber.e("foreceCloseWhenLocked");

        this.close();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {

        }

        this.reopen();
    }

    public void executeBlock(SqliteBlock<SQLiteDatabase> block) throws Exception{
        Timber.d("%s db-%s update: %s", this, getTag(), database.getPath());
        this.database.beginTransactionWithListener(new SQLiteTransactionListener() {
            @Override
            public void onBegin() {
                Timber.d("%s db-%s Transaction begin: %s", this, getTag(), new Date());
            }

            @Override
            public void onCommit() {
                Timber.d("%s db-%s Transaction commit: %s", this, getTag(), new Date());
            }

            @Override
            public void onRollback() {
                Timber.d("%s db-%s Transaction rollback: %s", this, getTag(), new Date());
            }
        });
        block.execute(this.database);
    }

    /**
     *
     * @param block
     */
    public void query(String tag, SqliteBlock<SQLiteDatabase> block){
        final long ts = System.currentTimeMillis();
        ensureDbOpen();
        try {
            Timber.d("db-%s query: %s, %s", getTag(), database.getPath(), tag);
            block.execute(this.database);
        } catch (Exception e) {
            Timber.e(e, "query Error. db-%s, %s", getTag(), tag);
        }finally {
            long ts0 = System.currentTimeMillis() - ts;
            Timber.i("db-%s query complete duration: %s ms, tag: %s", getTag(), ts0, tag);
        }
    }

    /**
     * 关闭
     */
    public synchronized void close(){
        if (database != null){
            Timber.d("db-%s close: %s", getTag(), database.getPath());
            database.close();
            SQLiteDatabase.releaseMemory();
            database = null;
        }
    }

    /**
     * 重新打开
     */
    public synchronized void reopen(){
        close();
        this.name = this.originalName;
        ensureDbOpen();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SqliteContext{");
        sb.append("path=").append(path);
        sb.append(", tag='").append(tag).append('\'');
        sb.append(", userId='").append(userId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
