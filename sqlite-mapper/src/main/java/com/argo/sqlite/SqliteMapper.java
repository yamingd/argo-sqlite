package com.argo.sqlite;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;
import net.sqlcipher.database.SQLiteStatementBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * Created by user on 8/13/15.
 */
public abstract class SqliteMapper<T, PKType> {

    public static final String S_COMMOA = ", ";
    public static final String S_QMARK = "?, ";
    public static final String S_OR = " OR ";
    public static final String S_AND = " AND ";
    public static final String S_E_Q = " = ? ";
    public static final String SELECT = "select ";
    public static final String FROM = " from ";
    public static final String ORDER_BY = " order by ";
    public static final String WHERE = " where ";
    public static final String LIMIT_OFFSET = " limit ? offset ?";
    public static final String DELETE_FROM = "delete from ";
    public static final String UPDATE = "update ";
    public static final String SET = " set ";
    public static final String S_EMPTY = " ";
    public static final String STRING_NULL = "";

    protected SqliteContext dbContext;

    protected String SELECT_FIELDS;
    protected String getStatement;

    protected SQLiteStatement insertStatement;
    protected SQLiteStatementBinder insertStatementBinder;

    protected SQLiteStatement deleteStatement;

    /**
     *
     */
    public SqliteMapper() {

        insertStatementBinder = new SQLiteStatementBinder() {
            @Override
            public void bind(SQLiteStatement statement, Object o) {
                bindInsertStatement(statement, (T)o);
            }
        };

    }

    /**
     * 遍历所有的记录
     * @return
     */
    protected List<T> loadRecords(Cursor cursor){
        List<T> list = new ArrayList<T>();
        if (cursor.getCount() == 0){
            cursor.close();
            return list;
        }

        if (cursor.isBeforeFirst()){
            cursor.moveToFirst();
        }

        while (true){
            T item = null;
            try {
                item = this.map(cursor, item);
            } catch (Exception e) {
                Timber.e(e, "mapping record error. %s" + this.getClass());
                cursor.close();
                return Collections.emptyList();
            }
            list.add(item);
            //Timber.d("loadRecords, " + item);
            if(!cursor.moveToNext()){
                break;
            }
        }

        cursor.close();
        return list;
    }

    protected void ensureContext(){
        if (this.dbContext == null){
            this.dbContext = SqliteEngine.find(this.getDbContextTag());
        }
    }

    public void resetStatement(){
        this.deleteStatement = null;
        this.insertStatement = null;
    }

    /**
     * 获取绑定的数据库
     * @return
     */
    public SQLiteDatabase getDatabase(){
        this.ensureContext();
        return this.dbContext.getDatabase();
    }

    /**
     * 准备好数据库和关联的Mapper
     */
    public void prepare(){
        this.ensureContext();
    }

    /**
     * 获得所有字段列表
     * @return
     */
    public abstract List<String> getColumns();

    /**
     * 获得主键字段名
     * @return
     */
    public abstract String getPkColumn();

    /**
     * 获得表名
     * @return
     */
    public abstract String getTableName();

    /**
     *
     * @return
     */
    public abstract Class<T> getClassType();

    /**
     *
     * @return
     */
    public abstract String getDbContextTag();
    /**
     * 构造select字段
     * @return
     */
    public String getSelectFields(){
        if (SELECT_FIELDS == null){

            List<String> columns = this.getColumns();
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < columns.size(); i++) {
                s.append(columns.get(i)).append(S_COMMOA);
            }

            s.setLength(s.length() - S_COMMOA.length());

            SELECT_FIELDS = s.toString();
        }

        return SELECT_FIELDS;
    }

    /**
     *
     */
    protected synchronized void compileInsertStatement(){
        if (insertStatement != null){
            return;
        }

        StringBuilder s = new StringBuilder("REPLACE into ");
        s.append(this.getTableName()).append("(");
        List<String> columns = this.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            s.append(columns.get(i)).append(S_COMMOA);
        }
        s.setLength(s.length() - S_COMMOA.length());
        s.append(")values(");
        for (int i = 0; i < columns.size(); i++) {
            s.append(S_QMARK);
        }
        s.setLength(s.length() - 2);
        s.append(")");

        Timber.d("save sql: %s", s.toString());

        SQLiteDatabase database = this.getDatabase();
        insertStatement = database.compileStatement(s.toString());
    }

    /**
     *
     */
    protected synchronized void compileDeleteStatement(){
        if (deleteStatement != null){
            return;
        }

        SQLiteDatabase database = this.getDatabase();
        String sql = String.format("delete from %s where %s = ?", this.getTableName(), this.getPkColumn());
        deleteStatement = database.compileStatement(sql);
    }

    /**
     * 0: false, 1: true
     * @param cursor
     * @param index
     * @return
     */
    protected Boolean getBoolean(Cursor cursor, int index){
        int val = cursor.getInt(index);
        return val == 1;
    }

    /**
     *
     * @param cursor
     * @param index
     * @return
     */
    protected Date getDate(Cursor cursor, int index){
        long ts = cursor.getLong(index);
        if (ts == 0){
            return null;
        }
        return new Date(ts * 1000);
    }

    /**
     *
     * @param date
     * @return
     */
    protected long getDate(Date date){
        if (date == null){
            return 0;
        }
        return date.getTime() / 1000;
    }

    protected int getBoolean(boolean val){
        return val ? 1 : 0;
    }

    protected String filterNull(String val){
        if (val == null){
            return STRING_NULL;
        }
        return val;
    }

    /**
     * 按主键读取
     * @param pkValue
     * @return
     */
    public T get(PKType pkValue){
        if (getStatement == null) {
            StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
            s.append(WHERE).append(this.getPkColumn()).append(S_E_Q);
            getStatement = s.toString();
        }

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(getStatement, new String[]{ pkValue + "" });
        List<T> list = loadRecords(cursor);
        if (list.size() == 0){
            return null;
        }

        return list.get(0);

    }
    /**
     * 按主键数组读取
     * @param pkValues
     * @return
     */
    public List<T> gets(Set<PKType> pkValues){
        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(WHERE);

        Iterator<PKType> itor = pkValues.iterator();
        String[] args = new String[pkValues.size()];
        int i = 0;
        while (itor.hasNext()){
            args[i] = itor.next().toString();
            s.append(this.getPkColumn()).append(S_E_Q);
            s.append(S_OR);
            i++;
        }
        s.setLength(s.length() - S_OR.length());

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(s.toString(), args);
        List<T> list = loadRecords(cursor);
        return list;

    }

    /**
     *
     * 按逗号分隔读取
     * @param idWithComma
     * @return
     */
    public List<T> gets(String idWithComma){
        String[] args = idWithComma.split(",");
        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(WHERE);

        for (int i = 0; i < args.length; i++) {
            s.append(this.getPkColumn()).append(S_E_Q);
            s.append(S_OR);
        }
        s.setLength(s.length() - S_OR.length());

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(s.toString(), args);
        List<T> list = loadRecords(cursor);
        return list;
    }

    /**
     *
     * @param pkValue
     * @return
     */
    public T getWithRef(PKType pkValue){
        T o = get(pkValue);
        this.wrapRef(o);
        return o;
    }

    /**
     * 按主键读取并读取关联实体
     * @param pkValues
     * @return
     */
    public List<T> getsWithRef(Set<PKType> pkValues){
        List<T> list = this.gets(pkValues);
        wrapRef(list);
        return list;
    }

    /**
     *
     * @param pkValues
     * @return
     */
    public List<T> getsWithRef(String pkValues){
        List<T> list = this.gets(pkValues);
        wrapRef(list);
        return list;
    }

    /**
     *
     * @param statement
     * @param o
     */
    protected abstract void bindInsertStatement(SQLiteStatement statement, T o);

    /**
     * 获取已编译的插入语句
     * @return
     */
    public boolean save(T o){
        if (o == null){
            return false;
        }
        if (insertStatement == null){
            this.compileInsertStatement();
        }
        this.insertStatementBinder.bind(insertStatement, o);
        int recs = insertStatement.executeUpdateDelete();
        return (recs == 1);
    }

    /**
     * 保存列表
     * @param list
     * @return
     */
    public boolean save(List<T> list){
        if (null == list || list.size() == 0){
            return false;
        }

        if (insertStatement == null){
            this.compileInsertStatement();
        }

        this.insertStatement.executeUpdateDelete(list, this.insertStatementBinder);

//        for (int i = 0; i < list.size(); i++) {
//            T o = list.get(i);
//            if (null != o){
//                this.save(o);
//            }
//        }

        return true;
    }

    /**
     * 保存列表
     * @param list
     * @return
     */
    public boolean saveWithRef(List<T> list){
        if (null == list || list.size() == 0){
            return false;
        }

        if (insertStatement == null){
            this.compileInsertStatement();
        }

        this.insertStatement.executeUpdateDelete(list, this.insertStatementBinder);

        //TODO:Ref的保存在子类实现

//        for (int i = 0; i < list.size(); i++) {
//            T o = list.get(i);
//            if (null != o){
//                this.saveWithRef(o);
//            }
//        }
        return true;
    }

    /**
     * 保存Set
     * @param set
     * @return
     */
    public boolean saveWithRef(Set<T> set){
        if (null == set || set.size() == 0){
            return false;
        }

        if (insertStatement == null){
            this.compileInsertStatement();
        }

        this.insertStatement.executeUpdateDelete(set, this.insertStatementBinder);

        //TODO: Ref的保存在子类实现

//        Iterator<T> iterator = list.iterator();
//        while (iterator.hasNext()){
//            T o = iterator.next();
//            if (null != o){
//                this.saveWithRef(o);
//            }
//        }

        return true;
    }


    /**
     *
     * @param o
     * @return
     */
    public boolean saveWithRef(T o){
        boolean ret = this.save(o);
        //TODO:Ref的保存在子类实现
        return ret;
    }


    /**
     * 按主键删除
     * @param o
     * @return
     */
    public abstract boolean delete(T o);
    /**
     *
     * @param cursor
     * @param o
     */
    public abstract T map(Cursor cursor, T o);

    /**
     * 删除where
     * @param where
     * @param args
     */
    public void deleteWhere(String where, Object[] args){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(DELETE_FROM).append(this.getTableName());
        s.append(WHERE).append(where);

        database.execSQL(s.toString(), args);
    }

    /**
     * 删除
     */
    public void deleteBy(PKType pkValue){

        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(DELETE_FROM).append(this.getTableName());
        s.append(WHERE).append(getPkColumn()).append(S_E_Q);

        database.execSQL(s.toString(), new Object[]{pkValue});

    }

    /**
     *
     * @return
     */
    public int count(){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append("count (1)").append(FROM).append(this.getTableName());
        int ret = 0;
        Cursor cursor = database.rawQuery(s.toString(), null);
        if (cursor.moveToFirst()){
            ret = cursor.getInt(0);
        }
        cursor.close();
        return ret;
    }

    /**
     *
     * @param groupBy
     * @return
     */
    public int count(String groupBy){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append("count (1)").append(FROM).append(this.getTableName());
        s.append(" group by ").append(groupBy);
        int ret = 0;
        Cursor cursor = database.rawQuery(s.toString(), null);
        if (cursor.moveToFirst()){
            ret = cursor.getInt(0);
        }
        cursor.close();
        return ret;
    }

    /**
     *
     * @param where
     * @return
     */
    public int countWhere(String where, String[] params){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append("count (1)").append(FROM).append(this.getTableName());
        s.append(WHERE).append(where);
        int ret = 0;
        Cursor cursor = database.rawQuery(s.toString(), params);
        if (cursor.moveToFirst()){
            ret = cursor.getInt(0);
        }
        cursor.close();
        return ret;
    }

    /**
     *
     * @param where
     * @param groupBy
     * @return
     */
    public int countWhere(String where, String groupBy, String[] params){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append("count (1)").append(FROM).append(this.getTableName());
        s.append(WHERE).append(where).append(" group by ").append(groupBy);
        int ret = 0;
        Cursor cursor = database.rawQuery(s.toString(), params);
        if (cursor.moveToFirst()){
            ret = cursor.getInt(0);
        }
        cursor.close();
        return ret;
    }

    /**
     *
     * @param field
     * @param where
     * @return
     */
    public int sumWhere(String field, String where, String[] params){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append("sum (").append(field).append(")").append(FROM).append(this.getTableName());
        s.append(WHERE).append(where);
        int ret = 0;
        Cursor cursor = database.rawQuery(s.toString(), params);
        if (cursor.moveToFirst()){
            ret = cursor.getInt(0);
        }
        cursor.close();
        return ret;
    }

    /**
     *
     * @param field
     * @param where
     * @param groupBy
     * @return
     */
    public int sumWhere(String field, String where, String groupBy, String[] params){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append("sum ()").append(field).append(")").append(FROM).append(this.getTableName());
        s.append(WHERE).append(where).append(" group by ").append(groupBy);
        int ret = 0;
        Cursor cursor = database.rawQuery(s.toString(), params);
        if (cursor.moveToFirst()){
            ret = cursor.getInt(0);
        }
        cursor.close();
        return ret;
    }

    /**
     * 查询全部
     * @return
     */
    public List<T> select(){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(ORDER_BY).append(this.getPkColumn());

        Cursor cursor = database.rawQuery(s.toString(), null);
        List<T> list = loadRecords(cursor);

        return list;
    }

    /**
     * 查询全部
     * @param order
     * @return
     */
    public List<T> select(String order){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(ORDER_BY).append(order);

        Cursor cursor = database.rawQuery(s.toString(), null);
        List<T> list = loadRecords(cursor);

        return list;
    }

    /**
     *
     * @param order
     * @param params
     * @return
     */
    public List<T> selectLimit(String order, String[] params){

        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(ORDER_BY).append(order).append(LIMIT_OFFSET);

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(s.toString(), params);
        List<T> list = loadRecords(cursor);

        return list;

    }

    /**
     * 查询数据
     * @param where
     * @param params
     * @return
     */
    public List<T> select(String where, String[] params){

        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(WHERE).append(where).append(ORDER_BY).append(this.getPkColumn()).append(" desc ");

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(s.toString(), params);
        List<T> list = loadRecords(cursor);

        return list;
    }

    /**
     * 查询数据
     * @param where
     * @param order
     * @param params
     * @return
     */
    public List<T> select(String where, String order, String[] params){

        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(WHERE).append(where).append(ORDER_BY).append(order);

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(s.toString(), params);
        List<T> list = loadRecords(cursor);

        return list;

    }

    /**
     * 查询数据
     * @param where
     * @param order
     * @param params
     * @return
     */
    public List<T> selectLimit(String where, String order, String[] params){

        StringBuilder s = new StringBuilder(SELECT).append(getSelectFields()).append(FROM).append(this.getTableName());
        s.append(WHERE).append(where).append(ORDER_BY).append(order).append(LIMIT_OFFSET);

        SQLiteDatabase database = this.getDatabase();
        Cursor cursor = database.rawQuery(s.toString(), params);
        List<T> list = loadRecords(cursor);

//        Timber.d("selectLimit:%s",s.toString());

        return list;

    }

    /**
     * 读取关联的实体
     * @param list
     */
    public abstract void wrapRef(List<T> list);

    /**
     * 读取关联的实体
     * @param o
     */
    public abstract void wrapRef(T o);

    /**
     * 查询全部（并查询关联实体)
     * @return
     */
    public List<T> selectWithRef(){
        List<T> list = select();
        this.wrapRef(list);
        return list;
    }

    /**
     *
     * @return
     */
    public List<T> selectWithRef(String order){
        List<T> list = select(order);
        this.wrapRef(list);
        return list;
    }

    /**
     *
     * @return
     */
    public List<T> selectLimitWithRef(String order, String[] params){
        List<T> list = selectLimit(order, params);
        this.wrapRef(list);
        return list;
    }

    /**
     * 查询数据（并查询关联实体)
     * @param where
     * @param params
     * @return
     */
    public List<T> selectWithRef(String where, String[] params){
        List<T> list = select(where, params);
        this.wrapRef(list);
        return list;
    }

    /**
     * 查询数据（并查询关联实体)
     * @param where
     * @param order
     * @param params
     * @return
     */
    public List<T> selectWithRef(String where, String order, String[] params){
        List<T> list = select(where, order, params);
        this.wrapRef(list);
        return list;
    }

    /**
     * 查询数据（并查询关联实体)
     * @param where
     * @param order
     * @param params
     * @return
     */
    public List<T> selectLimitWithRef(String where, String order, String[] params){
        List<T> list = selectLimit(where, order, params);
        this.wrapRef(list);
        return list;
    }

    /**
     *
     * @param value
     * @param where
     * @param params
     */
    public int update(String value, String where, Object[] params){
        SQLiteDatabase database = this.getDatabase();
        StringBuilder s = new StringBuilder(UPDATE).append(this.getTableName()).append(SET);
        s.append(value).append(S_EMPTY).append(WHERE).append(where);
        int ret = database.execSQL(s.toString(), params);
        Timber.d("update:" + s.toString());
        return ret;
    }

    /**
     *
     * @param where
     * @param params
     */
    public int delete(String where, Object[] params){
        SQLiteDatabase database = this.getDatabase();
        int ret = database.delete(this.getTableName(), where, params);
        return ret;
    }
}
