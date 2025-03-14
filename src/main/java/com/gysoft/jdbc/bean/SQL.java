package com.gysoft.jdbc.bean;

import com.gysoft.jdbc.dao.EntityDao;
import com.gysoft.jdbc.tools.CollectionUtil;
import com.gysoft.jdbc.tools.EntityTools;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author 周宁
 */
public class SQL extends AbstractCriteria<SQL> {
    /**
     * sql类型
     */
    private String sqlType;
    /**
     * 表名称
     */
    private String tbName;
    /**
     * 表的别名
     */
    private String aliasName;
    /**
     * 将sql作为表的别名
     */
    private String asTable;

    /**
     * 标识从from(String asTable,SQL c)
     * 方法传递asTable，此方法用于给子查询起别名
     */
    private boolean fromAsTable = false;

    /**
     * 删除语句中表的别名
     */
    private String deleteAliasName;
    /**
     * 连接
     */
    private List<Joins.BaseJoin> joins;
    /**
     * 子查询条件
     */
    private List<SQL> subSqls;
    /**
     * sql管道拼接
     */
    private SQLPiepline sqlPiepline = new SQLPiepline(this);
    /**
     * 表元数据
     */
    private TableMeta tableMeta;
    /**
     * 被查询的字段
     */
    private List<Object> selectFields;
    /**
     * 插入数据
     */
    private List<Object[]> insertValues;

    /**
     * 插入sql
     */
    private Pair<String, List<String>> insert;

    /**
     * 待更新的字段和相应的值
     */
    private List<Pair> kvs;
    /**
     * 连接类型
     */
    private String unionType;

    /**
     * 喝醉了的，代表人很糊涂删除表或者清楚数据
     */
    private Drunk drunk;

    public SQL() {
        selectFields = new ArrayList<>();
        kvs = new ArrayList<>();
        joins = new ArrayList<>();
        subSqls = new ArrayList<>();
        insertValues = new ArrayList<>();
        insert = new Pair<>();
    }

    public SQL from(SQL... cc) {
        for (SQL c : cc) {
            c.getSqlPiepline().getSqlNexts().forEach(sqlNext -> {
                SQL s = sqlNext.getSql();
                if (sqlNext.getUnionType() != null) {
                    s.setUnionType(sqlNext.getUnionType());
                } else {
                    s.setUnionType(",");
                }
                subSqls.add(s);
            });
        }
        return this;
    }

    //此方法传参的asTable优先级高于asTable()方法
    public SQL from(SQL c, String asTable) {
        this.asTable = asTable;
        this.fromAsTable = true;
        c.getSqlPiepline().getSqlNexts().forEach(sqlNext -> {
            SQL s = sqlNext.getSql();
            if (sqlNext.getUnionType() != null) {
                s.setUnionType(sqlNext.getUnionType());
            } else {
                s.setUnionType(",");
            }
            subSqls.add(s);
        });
        return this;
    }

    public SQL union() {
        SQL next = new SQL();
        sqlPiepline.add(next, "UNION");
        next.setSqlPiepline(sqlPiepline);
        return next;
    }

    public SQL unionAll() {
        SQL next = new SQL();
        sqlPiepline.add(next, "UNION ALL");
        next.setSqlPiepline(sqlPiepline);
        return next;
    }

    public List<Object> getSelectFields() {
        return selectFields;
    }

    public void setSelectFields(List<Object> selectFields) {
        this.selectFields = selectFields;
    }

    public List<Pair> getKvs() {
        return kvs;
    }

    public void setKvs(List<Pair> kvs) {
        this.kvs = kvs;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public void setJoins(List<Joins.BaseJoin> joins) {
        this.joins = joins;
    }

    public SQLPiepline getSqlPiepline() {
        return sqlPiepline;
    }

    public void setSqlPiepline(SQLPiepline sqlPiepline) {
        this.sqlPiepline = sqlPiepline;
    }

    public List<SQL> getSubSqls() {
        return subSqls;
    }

    public void setSubSqls(List<SQL> subSqls) {
        this.subSqls = subSqls;
    }

    public String getDeleteAliasName() {
        return deleteAliasName;
    }

    public void setDeleteAliasName(String deleteAliasName) {
        this.deleteAliasName = deleteAliasName;
    }

    public String getAsTable() {
        return asTable;
    }

    public void setAsTable(String asTable) {
        this.asTable = asTable;
    }

    public boolean getFromAsTable() {
        return fromAsTable;
    }

    public void setFromAsTable(boolean fromAsTable) {
        this.fromAsTable = fromAsTable;
    }

    public SQL select(Object... fields) {
        selectFields.addAll(Arrays.stream(fields).collect(Collectors.toList()));
        //复合查询insert select语法BUG修复
        if (this.sqlType == null) {
            this.sqlType = EntityDao.SQL_SELECT;
        }
        return this;
    }

    public <T, R> SQL select(TypeFunction<T, R>... functions) {
        selectFields.addAll(Arrays.stream(functions).map(function -> TypeFunction.getLambdaColumnName(function)).collect(Collectors.toList()));
        //复合查询insert select语法BUG修复
        if (this.sqlType == null) {
            this.sqlType = EntityDao.SQL_SELECT;
        }
        return this;
    }

    public SQL update(String table) {
        this.tbName = table;
        this.sqlType = EntityDao.SQL_UPDATE;
        return this;
    }

    public SQL update(String table,String aliasName){
        this.aliasName = aliasName;
        return update(table);
    }

    public SQL update(Class clss) {
        this.tbName = EntityTools.getTableName(clss);
        this.sqlType = EntityDao.SQL_UPDATE;
        return this;
    }

    public SQL update(Class clss,String aliasName){
        this.aliasName = aliasName;
        return update( EntityTools.getTableName(clss));
    }

    public SQL delete(String deleteAliasName) {
        this.deleteAliasName = deleteAliasName;
        this.sqlType = EntityDao.SQL_DELETE;
        return this;
    }

    public SQL delete(String... deleteAliasNames) {
        this.deleteAliasName = Arrays.stream(deleteAliasNames).collect(Collectors.joining(","));
        this.sqlType = EntityDao.SQL_DELETE;
        return this;
    }

    public SQL delete() {
        this.sqlType = EntityDao.SQL_DELETE;
        return this;
    }

    public SQL insertInto(String table, String... fields) {
        insert.setFirst(table);
        insert.setSecond(Arrays.stream(fields).collect(Collectors.toList()));
        this.sqlType = EntityDao.SQL_INSERT;
        return this;
    }

    public SQL replaceInto(String table, String... fields) {
        insert.setFirst(table);
        insert.setSecond(Arrays.stream(fields).collect(Collectors.toList()));
        this.sqlType = EntityDao.SQL_REPLACE;
        return this;
    }

    public SQL insertIgnoreInto(String table, String... fields) {
        insert.setFirst(table);
        insert.setSecond(Arrays.stream(fields).collect(Collectors.toList()));
        this.sqlType = EntityDao.SQL_INSERTIGNORE;
        return this;
    }


    public <T, R> SQL insertInto(String table, TypeFunction<T, R>... functions) {
        insert.setFirst(table);
        insert.setSecond(Arrays.stream(functions).map(f -> TypeFunction.getLambdaColumnName(f)).collect(Collectors.toList()));
        this.sqlType = EntityDao.SQL_INSERT;

        return this;
    }

    public <T, R> SQL replaceInto(String table, TypeFunction<T, R>... functions) {
        insert.setFirst(table);
        insert.setSecond(Arrays.stream(functions).map(f -> TypeFunction.getLambdaColumnName(f)).collect(Collectors.toList()));
        this.sqlType = EntityDao.SQL_REPLACE;
        return this;
    }


    public <T, R> SQL insertIgnoreInto(String table, TypeFunction<T, R>... functions) {
        insert.setFirst(table);
        insert.setSecond(Arrays.stream(functions).map(f -> TypeFunction.getLambdaColumnName(f)).collect(Collectors.toList()));
        this.sqlType = EntityDao.SQL_INSERTIGNORE;
        return this;
    }


    public SQL insertInto(String table) {
        insert.setFirst(table);
        this.sqlType = EntityDao.SQL_INSERT;
        return this;
    }

    public SQL replaceInto(String table) {
        insert.setFirst(table);
        this.sqlType = EntityDao.SQL_REPLACE;
        return this;
    }


    public SQL insertIgnoreInto(String table) {
        insert.setFirst(table);
        this.sqlType = EntityDao.SQL_INSERTIGNORE;
        return this;
    }

    public <T, R> SQL insertInto(Class clss, String... fields) {
        return insertInto(EntityTools.getTableName(clss), fields);
    }

    public <T, R> SQL replaceInto(Class clss, String... fields) {
        return replaceInto(EntityTools.getTableName(clss), fields);
    }

    public <T, R> SQL insertInto(Class clss, TypeFunction<T, R>... functions) {
        return insertInto(EntityTools.getTableName(clss), functions);
    }

    public <T, R> SQL replaceInto(Class clss, TypeFunction<T, R>... functions) {
        return replaceInto(EntityTools.getTableName(clss), functions);
    }

    public <T, R> SQL insertInto(Class clss) {
        return insertInto(EntityTools.getTableName(clss));
    }

    public <T, R> SQL replaceInto(Class clss) {
        return replaceInto(EntityTools.getTableName(clss));
    }

    public <T, R> SQL insertIgnoreInto(Class clss, String... fields) {
        return insertIgnoreInto(EntityTools.getTableName(clss), fields);
    }

    public <T, R> SQL insertIgnoreInto(Class clss, TypeFunction<T, R>... functions) {
        return insertIgnoreInto(EntityTools.getTableName(clss), functions);
    }

    public <T, R> SQL insertIgnoreInto(Class clss) {
        return insertIgnoreInto(EntityTools.getTableName(clss));
    }

    public SQL truncate() {
        sqlType = EntityDao.SQL_TRUNCATE;
        drunk = new Drunk();
        return this;
    }

    public SQL table(String... tables) {
        drunk.setTables(Arrays.stream(tables).collect(Collectors.toSet()));
        return this;
    }

    public SQL table(Class... clss) {
        drunk.setTables(Arrays.stream(clss).map(EntityTools::getTableName).collect(Collectors.toSet()));
        return this;
    }

    public SQL drop() {
        sqlType = EntityDao.SQL_DROP;
        drunk = new Drunk();
        return this;
    }

    public SQL ifExists() {
        drunk.setIfExists(true);
        return this;
    }

    public SQL set(String key, Object value) {
        kvs.add(new Pair(key, value));
        return this;
    }

    public <T, R> SQL set(TypeFunction<T, R> function, Object value) {
        kvs.add(new Pair(TypeFunction.getLambdaColumnName(function), value));
        return this;
    }

    public SQL as(String aliasName) {
        this.aliasName = aliasName;
        return this;
    }

    public SQL asTable(String aliasName) {
        this.asTable = aliasName;
        return this;
    }

    public SQL from(Class clss) {
        tbName = EntityTools.getTableName(clss);
        return this;
    }

    public SQL from(String tbName) {
        this.tbName = tbName;
        return this;
    }

    public SQL from(String tbName, String aliasName) {
        this.tbName = tbName;
        return as(aliasName);
    }

    public SQL from(Class clss, String aliasName) {
        tbName = EntityTools.getTableName(clss);
        return as(aliasName);
    }

    public SQL leftJoin(Joins.On on) {
        on.setJoinType(JoinType.LeftJoin);
        return join(on);
    }

    public SQL rightJoin(Joins.On on) {
        on.setJoinType(JoinType.RightJoin);
        return join(on);
    }

    public SQL innerJoin(Joins.On on) {
        on.setJoinType(JoinType.InnerJoin);
        return join(on);
    }


    public SQL natureJoin(Joins.BaseJoin as) {
        as.setJoinType(JoinType.NatureJoin);
        joins.add(as);
        return this;
    }

    private SQL join(Joins.On join) {
        joins.add(join);
        return this;
    }

    public SQL join(JoinType joinType, Object table, String aliasName) {
        Joins.As as = null;
        if (table instanceof Class) {
            as = new Joins().with((Class) table).as(aliasName);
        } else if (table instanceof String) {
            as = new Joins().with((String) table).as(aliasName);
        } else if (table instanceof SQL) {
            as = new Joins().with((SQL) table).as(aliasName);
        }
        as.setJoinType(joinType);
        joins.add(as);
        return this;
    }

    public SQL leftJoin(Object table, String aliasName) {
        return join(JoinType.LeftJoin, table, aliasName);
    }

    public SQL rightJoin(Object table, String aliasName) {
        return join(JoinType.RightJoin, table, aliasName);
    }

    public SQL innerJoin(Object table, String aliasName) {
        return join(JoinType.InnerJoin, table, aliasName);
    }

    public SQL natureJoin(Object table, String aliasName) {
        return join(JoinType.NatureJoin, table, aliasName);
    }

    public SQL on(String field, String field2) {
        Object as = joins.get(joins.size() - 1);
        if (as instanceof Joins.As) {
            joins.remove(as);
            Joins.On on = ((Joins.As) as).on(field, field2);
            joins.add(on);
        } else {
            ((Joins.On) as).on(field, field2);
        }
        return this;
    }

    public SQL on(String field, String opt, Object field2) {
        Object as = joins.get(joins.size() - 1);
        if (as instanceof Joins.On) {
            ((Joins.On) as).and(field, opt, field2);
        } else {
            throw new RuntimeException("the sql has no join condition");
        }
        return this;
    }

    public String getTbName() {
        return tbName;
    }

    public void setTbName(String tbName) {
        this.tbName = tbName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public List<Joins.BaseJoin> getJoins() {
        return joins;
    }

    public SQL values(Object... values) {
        insertValues.add(values);
        return this;
    }

    public SQL values(List<Object[]> values) {
        insertValues.addAll(values);
        return this;
    }

    public Table create() {
        return new Table(this);
    }

    public void setTableMeta(TableMeta tableMeta) {
        this.tableMeta = tableMeta;
    }

    public TableMeta getTableMeta() {
        return tableMeta;
    }


    public String getSqlType() {
        return sqlType;
    }

    public void setSqlType(String sqlType) {
        this.sqlType = sqlType;
    }

    public String getUnionType() {
        return unionType;
    }

    public void setUnionType(String unionType) {
        this.unionType = unionType;
    }

    public Drunk getDrunk() {
        return drunk;
    }

    public void setDrunk(Drunk drunk) {
        this.drunk = drunk;
    }

    public List<Object[]> getInsertValues() {
        return insertValues;
    }

    public void setInsertValues(List<Object[]> insertValues) {
        this.insertValues = insertValues;
    }

    public Pair<String, List<String>> getInsert() {
        return insert;
    }

    public void setInsert(Pair<String, List<String>> insert) {
        this.insert = insert;
    }

    public SQL onDuplicateKeyUpdate(String key, Object value) {
        kvs.add(new Pair(key, value));
        return this;
    }

    public <T, R> SQL onDuplicateKeyUpdate(TypeFunction<T, R> function, Object value) {
        kvs.add(new Pair(TypeFunction.getLambdaColumnName(function), value));
        return this;
    }

    public Map<String, Object> getUpdates() {
        Map<String, Object> result = new HashMap<>();
        if (sqlType.equals(EntityDao.SQL_UPDATE) && CollectionUtils.isNotEmpty(kvs)) {
            kvs.forEach(pair -> {
                result.put(pair.getFirst().toString(), pair.getSecond());
            });
        }
        return result;
    }

}
