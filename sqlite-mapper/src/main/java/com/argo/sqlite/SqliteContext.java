package com.argo.sqlite;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteTransactionListener;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Date;

import timber.log.Timber;

/**
 * Created by user on 8/13/15.
 */
public class SqliteContext {

    public static final String SQLITE_TAG_DEFAULT = "default";
    public static final String SUFFIX = ".db";

    private File path;
    private String tag;
    private String name;
    private String userId;
    private String originalName;
    private byte[] salt;

    private SQLiteDatabase database;


    public SqliteContext(String name, File path, byte[] salt) {
        this.tag = SQLITE_TAG_DEFAULT;
        this.name = name;
        this.originalName = name;
        this.salt = salt;
        this.path = path;
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
    }

    public SQLiteDatabase getDatabase() {
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
        if (null == name){
            name = "user_" + this.getUserId();
        }

        if (database != null){
            return;
        }

        open(name);
    }

    /**
     * 新建表
     * @param sql
     */
    public void createTable(String sql){
        ensureDbOpen();
        this.database.rawExecSQL(sql);
        int v = this.database.getVersion();
        this.database.setVersion(v + 1);
    }

    /**
     * 更新表
     * @param sql
     */
    public void upgradeTable(String sql){
        ensureDbOpen();
        this.database.rawExecSQL(sql);
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
            Timber.d("db-%s update: %s", getTag(), database.getPath());
            this.database.beginTransactionWithListener(new SQLiteTransactionListener() {
                @Override
                public void onBegin() {
                    Timber.d("db-%s Transaction begin: %s", getTag(), new Date());
                }

                @Override
                public void onCommit() {
                    Timber.d("db-%s Transaction commit: %s", getTag(), new Date());
                }

                @Override
                public void onRollback() {
                    Timber.d("db-%s Transaction rollback: %s", getTag(), new Date());
                }
            });
            block.execute(this.database);
        } catch (Exception e) {
            error = true;
            Timber.e(e, "update Error. db-%s", getTag());
        }finally {
            if (!error){
                this.database.setTransactionSuccessful();
            }
            this.database.endTransaction();
            long ts0 = System.currentTimeMillis() - ts;
            Timber.d("db-%s update complete duration: %s ms", getTag(), ts0);
        }
    }

    /**
     *
     * @param block
     */
    public void query(SqliteBlock<SQLiteDatabase> block){
        ensureDbOpen();
        try {
            Timber.d("db-%s query: %s", getTag(), database.getPath());
            block.execute(this.database);
        } catch (Exception e) {
            Timber.e(e, "query Error. db-%s", getTag());
        }
    }

    /**
     * 关闭
     */
    public void close(){
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
    public void reopen(){
        close();
        this.name = this.originalName;
        ensureDbOpen();
    }
}
