package com.argo.sqlite;

import com.argo.sqlite.annotations.RefLink;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

/**
 * Created by user on 8/14/15.
 */
public class SqliteMapperClassGenerator {

    private static ClassName mapperBaseClass = ClassName.bestGuess("com.argo.sqlite.SqliteMapper");
    private static ClassName SQLiteDatabaseClass = ClassName.get("net.sqlcipher.database", "SQLiteDatabase");

    private ProcessingEnvironment processingEnvironment;
    private ClassMetaData metadata;
    private final String className;

    // Class metadata for generating proxy classes
    private Elements elementUtils;
    private Types typeUtils;
    private ClassName entityClass;
    private ClassName engineClass;
    private ClassName simpleMapperClass;


    private TypeSpec.Builder builder;

    private String mapperClassName;
    private ClassName mapperTypeName;

    ClassName mapType = ClassName.get("java.util", "Map");
    ClassName arrayMap = ClassName.get("android.support.v4.util", "ArrayMap");
    ClassName setType = ClassName.get("java.util", "Set");
    ClassName hashSetType = ClassName.get("java.util", "HashSet");
    ClassName listType = ClassName.get("java.util", "List");
    ClassName iteratorType = ClassName.get("java.util", "Iterator");
    ClassName timberType = ClassName.get("timber.log", "Timber");

    private String typeStringName = "java.lang.String";

    public SqliteMapperClassGenerator(ProcessingEnvironment processingEnvironment, ClassMetaData metadata) {
        this.processingEnvironment = processingEnvironment;
        this.metadata = metadata;
        this.className = metadata.getSimpleClassName();
        this.mapperClassName = Utils.getMapperClassName(className);
        this.mapperTypeName = ClassName.bestGuess(this.mapperClassName);
    }

    public void generate() throws IOException, UnsupportedOperationException {
        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();

        engineClass = ClassName.bestGuess("com.argo.sqlite.SqliteEngine");
        simpleMapperClass = ClassName.bestGuess("com.argo.sqlite.SqliteMapper");

        entityClass = ClassName.bestGuess(this.metadata.getFullyQualifiedClassName());
        //Utils.note("entityClass: " + entityClass);

        SqliteMapperTypeName extMapperType = SqliteMapperTypeName.get(mapperBaseClass, entityClass, this.metadata.getPrimaryKeyTypeName());
        //Utils.note("extMapperType: " + extMapperType);

        //realmList = typeUtils.getDeclaredType(elementUtils.getTypeElement("io.realm.RealmList"), typeUtils.getWildcardType(null, null));

        String qualifiedGeneratedClassName = String.format("%s.%s", this.metadata.getPackageName(), Utils.getMapperClassName(className));
        JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(qualifiedGeneratedClassName);
        //File file = new File(sourceFile.toUri().getPath());
        //Utils.note("sourceFile: " + file);

        builder = TypeSpec.classBuilder(mapperClassName).addModifiers(Modifier.PUBLIC);
        builder.superclass(extMapperType);

        this.addStaticFields();
        this.addStaticInitCodes();
        this.addConstructor();
        this.addInsertStatementBinder();
        this.addInheritGetter();
        this.addPrepareMethod();
        this.addSaveWithRefMethod();
        this.addSaveWithListRefMethod();
        this.addSaveWithSetRefMethod();
        this.addDeleteMethod();
        this.addMapMethod();
        this.addWrapRefListMethod();
        this.addWrapRefMethod();

        JavaFile javaFile = JavaFile.builder(this.metadata.getPackageName(), builder.build())
                .build();

        final BufferedWriter bufferedWriter = new BufferedWriter(sourceFile.openWriter());
        javaFile.writeTo(bufferedWriter);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    static final String N_pkColumn = "pkColumn";
    static final String N_tableName = "tableName";
    static final String N_dbContextTag = "dbContextTag";
    static final String N_instance = "instance";

    /**
     * 声明静态成员
     */
    private void addStaticFields(){

        ListTypeName listTypeName = ListTypeName.of(String.class);

        builder.addField(String.class, N_pkColumn, Modifier.PRIVATE, Modifier.STATIC);
        builder.addField(String.class, N_tableName, Modifier.PRIVATE, Modifier.STATIC);
        builder.addField(String.class, N_dbContextTag, Modifier.PUBLIC, Modifier.STATIC);
        builder.addField(mapperTypeName, N_instance, Modifier.PUBLIC, Modifier.STATIC);
    }

    private void addStaticInitCodes(){
        CodeBlock.Builder block = CodeBlock.builder();

        block.addStatement("$N = $S", N_pkColumn, this.metadata.getPrimaryKey().getSimpleName());
        block.addStatement("$N = $S", N_tableName, this.metadata.getTableAnnotation().value());
        block.addStatement("$N = $S", N_dbContextTag, this.metadata.getTableAnnotation().context());

        builder.addStaticBlock(block.build());
    }

    private String buildCreateTableSql(){
        StringBuilder s = new StringBuilder(125);
        s.append("create table if not exists ").append(this.metadata.getTableAnnotation().value());
        s.append("( ");

        List<String> list = this.metadata.getFieldNames();
        String pkName = this.metadata.getPrimaryKey().getSimpleName().toString();
        for (int i = 0; i < list.size(); i++) {
            final String name = list.get(i);
            final String typeName = this.metadata.getFieldTypeName(i);
            //Utils.note("typeName: " + name + " : " + typeName);
            s.append(name).append(" ").append(Constants.JAVA_TO_SQLITE_TYPES.get(typeName));
            if (pkName.equalsIgnoreCase(name)){
                s.append(" PRIMARY KEY");
            }
            s.append(", ");
        }
        s.setLength(s.length() - 2);
        s.append(" ) WITHOUT ROWID ;");

        return s.toString();
    }

    private void addConstructor(){

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("super()")
                .addStatement("instance = this")
                .build();


        builder.addMethod(constructor);

    }

    private void addInsertStatementBinder(){

        MethodSpec.Builder method = MethodSpec.methodBuilder("bindInsertStatement")
                .addModifiers(Modifier.PROTECTED)
                .addParameter(ClassName.bestGuess("net.sqlcipher.database.SQLiteStatement"), "statement")
                .addParameter(entityClass, "o")
                .addAnnotation(Override.class);

        final List<String> fieldNames = this.metadata.getFieldNames();
        for (int i = 0; i < fieldNames.size(); i++) {
            int bindIndex = i + 1;
            String filedName = fieldNames.get(i);
            String typeName = this.metadata.getFieldTypeName(i);

            final String bind = Constants.JAVA_TO_BINDING.get(typeName);
            final String getter = this.metadata.getGetter(filedName);

            Utils.note("typeName:" + typeName + ", bind: " + bind + ", getter: " + getter);
            if (typeName.equalsIgnoreCase("date")){
                method.addStatement("statement.$N($N, getDate(o.$N()))", bind, bindIndex + "", getter);
            }else if(typeName.equalsIgnoreCase("boolean")){
                method.addStatement("statement.$N($N, getBoolean(o.$N()))", bind, bindIndex + "", getter);
            }else if (typeName.equalsIgnoreCase("string")){
                method.addStatement("statement.$N($N, filterNull(o.$N()))", bind, bindIndex + "", getter);
            }else{
                method.addStatement("statement.$N($N, o.$N())", bind, bindIndex + "", getter);
            }
        }

        builder.addMethod(method.build());

    }

    private void addInheritGetter(){

        //1.

        String N_columns = "columns";
        ListTypeName listTypeName = ListTypeName.of(String.class);
        MethodSpec.Builder getColumns = MethodSpec.methodBuilder("getColumns")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(listTypeName);

        ClassName className = ClassName.get("java.util", "ArrayList");
        ParameterizedTypeName listOf = ParameterizedTypeName.get(className, TypeName.get(String.class));
        getColumns.addStatement("$T $N = new $T()", listTypeName, N_columns, listOf);

        List<String> fieldNames = this.metadata.getFieldNames();
        for (int i = 0; i < fieldNames.size(); i++) {
            getColumns.addStatement("$N.add($S)", N_columns, fieldNames.get(i));
        }

        getColumns.addStatement("return $N", N_columns);


        builder.addMethod(getColumns.build());

        //2. getColumnInfo

        MapTypeName mapTypeName = new MapTypeName(mapType, TypeName.get(String.class), TypeName.get(String.class));
        MapTypeName arraymapTypeName = new MapTypeName(arrayMap, TypeName.get(String.class), TypeName.get(String.class));

        MethodSpec.Builder getColumnInfo = MethodSpec.methodBuilder("getColumnInfo")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(mapTypeName);

        getColumnInfo.addStatement("$T $N = new $T()", mapTypeName, N_columns, arraymapTypeName);

        for (int i = 0; i < fieldNames.size(); i++) {
            final String typeName = this.metadata.getFieldTypeName(i);
            getColumnInfo.addStatement("$N.put($S, $S)", N_columns, fieldNames.get(i), Constants.JAVA_TO_SQLITE_TYPES.get(typeName));
        }

        getColumnInfo.addStatement("return $N", N_columns);


        builder.addMethod(getColumnInfo.build());


        //2.
        MethodSpec getPkColumn = MethodSpec.methodBuilder("getPkColumn")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $N", N_pkColumn)
                .build();


        builder.addMethod(getPkColumn);

        //3.
        MethodSpec getTableName = MethodSpec.methodBuilder("getTableName")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $N", N_tableName)
                .build();


        builder.addMethod(getTableName);

        //4.
        MethodSpec getClassType = MethodSpec.methodBuilder("getClassType")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(EntityClassTypeName.get(entityClass))
                .addStatement("return $N.class", this.metadata.getClassType().getSimpleName().toString())
                .build();


        builder.addMethod(getClassType);

        //5.
        MethodSpec getDbContextTag = MethodSpec.methodBuilder("getDbContextTag")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return $N", N_dbContextTag)
                .build();


        builder.addMethod(getDbContextTag);

        MethodSpec.Builder getTableCreateSql = MethodSpec.methodBuilder("getTableCreateSql")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class);

        getTableCreateSql.addStatement("String sql = $S", this.buildCreateTableSql());
        getTableCreateSql.addStatement("return sql");


        builder.addMethod(getTableCreateSql.build());
    }

    private void addPrepareMethod(){

        MethodSpec.Builder prepare = MethodSpec.methodBuilder("prepare")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class);

        prepare.addStatement("super.prepare()");
        prepare.addStatement("this.dbContext.initTable(this)");

        builder.addMethod(prepare.build());
    }


    private void addSaveWithRefMethod(){

        MethodSpec.Builder save = MethodSpec.methodBuilder("saveWithRef")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityClass, "o")
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)

                .addStatement("boolean ret = super.saveWithRef(o)")

                .beginControlFlow("if (!ret)")
                .addStatement("return false")
                .endControlFlow();


        final List<VariableElement> refFields = this.metadata.getRefFields();
        final List<String> refNames = this.metadata.getRefFieldNames();

        String varName = "";

        for (int i = 0; i < refNames.size(); i++) {
            String name = refNames.get(i);
            String getter = this.metadata.getGetter(name);
            TypeMirror e = refFields.get(i).asType();

            varName = "refVar" + i;

            save.addStatement("// save $N", name);

            TypeName targetTypeName = TypeName.get(e);
            TypeName varTypeName = TypeName.get(e);

            if (isListType(e) || isSetType(e)){
                ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(e);
                targetTypeName = className.typeArguments.get(0);
            }

            ClassName targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetTypeName.toString()));
            save.addStatement("$T $N = o.$N()", varTypeName, varName, getter);
            save.beginControlFlow("if (null != $N)", varName);
            save.addStatement("$T.instance.saveWithRef($N)", targetMapperClass, varName);
            save.endControlFlow();
        }

        save.addStatement("return true");

        builder.addMethod(save.build());

    }

    private void addSaveWithListRefMethod(){

        MethodSpec.Builder save = MethodSpec.methodBuilder("saveWithRef")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ListTypeName.of(entityClass), "list")
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)

                .addStatement("boolean ret = super.saveWithRef(list)")

                .beginControlFlow("if (!ret)")
                .addStatement("return false")
                .endControlFlow();


        final List<VariableElement> refFields = this.metadata.getRefFields();
        final List<String> refNames = this.metadata.getRefFieldNames();

        if (refNames.size() == 0){
            return;
        }

        String varName = "";

        save.addStatement("List vars = new ArrayList()");

        for (int i = 0; i < refNames.size(); i++) {
            String name = refNames.get(i);
            String getter = this.metadata.getGetter(name);
            TypeMirror e = refFields.get(i).asType();

            varName = "refVar" + i;
            boolean isList = false;
            save.addStatement("$T.d($S)", timberType, name);

            TypeName targetTypeName = TypeName.get(e);
            TypeName varTypeName = TypeName.get(e);

            if (isListType(e) || isSetType(e)){
                isList = true;
                ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(e);
                targetTypeName = className.typeArguments.get(0);
            }

            ClassName targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetTypeName.toString()));

            save.beginControlFlow("for(int i=0; i<list.size(); i++)");
            save.addStatement("$T $N = list.get(i).$N()", varTypeName, varName, getter);
            save.beginControlFlow("if (null != $N)", varName);
            if (isList){
                save.addStatement("vars.addAll($N)", varName);
            }else {
                save.addStatement("vars.add($N)", varName);
            }

            save.endControlFlow();
            save.endControlFlow();
            save.addStatement("$T.instance.saveWithRef(vars)", targetMapperClass);
            save.addStatement("vars.clear()");

        }

        save.addStatement("return true");

        builder.addMethod(save.build());

    }

    private void addSaveWithSetRefMethod(){

        TypeName listOfClass = ParameterizedTypeName.get(setType, entityClass);

        MethodSpec.Builder save = MethodSpec.methodBuilder("saveWithRef")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(listOfClass, "set")
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)

                .addStatement("boolean ret = super.saveWithRef(set)")

                .beginControlFlow("if (!ret)")
                .addStatement("return false")
                .endControlFlow();


        final List<VariableElement> refFields = this.metadata.getRefFields();
        final List<String> refNames = this.metadata.getRefFieldNames();

        if (refNames.size() == 0){
            return;
        }

        String varName = "";

        save.addStatement("List vars = new ArrayList()");

        for (int i = 0; i < refNames.size(); i++) {
            String name = refNames.get(i);
            String getter = this.metadata.getGetter(name);
            TypeMirror e = refFields.get(i).asType();

            varName = "refVar" + i;
            boolean isList = false;
            save.addStatement("// save $N", name);

            TypeName targetTypeName = TypeName.get(e);
            TypeName varTypeName = TypeName.get(e);

            if (isListType(e) || isSetType(e)){
                isList = true;
                ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(e);
                targetTypeName = className.typeArguments.get(0);
            }

            ClassName targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetTypeName.toString()));

            save.addStatement("$T<$T> $N = set.iterator()", iteratorType, entityClass, varName);
            save.beginControlFlow("while ($N.hasNext())", varName);
            save.addStatement("$T v = $N.next().$N()", varTypeName, varName, getter);

            save.beginControlFlow("if (null != v)");
            if (isList){
                save.addStatement("vars.addAll(v)");
            }else {
                save.addStatement("vars.add(v)");
            }
            save.endControlFlow();

            save.endControlFlow();

            save.addStatement("$T.instance.saveWithRef(vars)", targetMapperClass);
            save.addStatement("vars.clear()");

        }

        save.addStatement("return true");

        builder.addMethod(save.build());

    }

    private boolean isListType(TypeMirror typeMirror){
        String str = typeMirror.toString();
        return str.startsWith("java.util.List");
    }

    private boolean isSetType(TypeMirror typeMirror){
        String str = typeMirror.toString();
        return str.startsWith("java.util.Set");
    }

    private void addDeleteMethod(){

        MethodSpec.Builder delete = MethodSpec.methodBuilder("delete")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(entityClass, "o")
                .addAnnotation(Override.class)
                .returns(TypeName.BOOLEAN)

                .beginControlFlow("if (o == null)")
                .addStatement("return false")
                .endControlFlow()

                .beginControlFlow("if (deleteStatement == null)")
                .addStatement("this.compileDeleteStatement()")
                .endControlFlow();


        String filedName = this.metadata.getPrimaryKey().getSimpleName().toString();
        String typeName = this.metadata.getFieldTypeName(this.metadata.getPrimaryKey());

        final String bind = Constants.JAVA_TO_BINDING.get(typeName);
        final String getter = this.metadata.getGetter(filedName);

        delete.addStatement("deleteStatement.$N($N, o.$N())", bind, "1", getter);

        delete.addStatement("int recs = deleteStatement.executeUpdateDelete()")
                .addStatement("return (recs == 1)");


        builder.addMethod(delete.build());

    }

    private void addMapMethod(){

        ClassName cursor = ClassName.bestGuess("net.sqlcipher.Cursor");

        String N_Cursor = "cursor";

        MethodSpec.Builder map = MethodSpec.methodBuilder("map")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(cursor, N_Cursor)
                .addParameter(entityClass, "o")
                .addAnnotation(Override.class)
                .returns(entityClass)

                .beginControlFlow("if (o == null)")
                .addStatement("o = new $T()", entityClass)
                .endControlFlow()

                .beginControlFlow("if ($N.isBeforeFirst())", N_Cursor)
                .addStatement("$N.moveToFirst()", N_Cursor)
                .endControlFlow();

        String getBoolean = "getBoolean";
        String getDate = "getDate";

        final List<String> fieldNames = this.metadata.getFieldNames();
        for (int i = 0; i < fieldNames.size(); i++) {
            String filedName = fieldNames.get(i);
            String typeName = this.metadata.getFieldTypeName(i);
            final String bind = Constants.JAVA_TO_SQLITE_GET.get(typeName);
            final String setter = this.metadata.getSetter(filedName);
            //Utils.note("typeName:" + typeName + ", bind: " + bind + ", getter: " + getter);
            if (bind.equalsIgnoreCase(getBoolean) || bind.equalsIgnoreCase(getDate)){
                map.addStatement("o.$N($N($N, $N))", setter, bind, N_Cursor, i + "");
            }else {
                map.addStatement("o.$N($N.$N($N))", setter, N_Cursor, bind, i + "");
            }
        }

        map.addStatement("return o");


        builder.addMethod(map.build());

    }

    private void addWrapRefListMethod(){

        final List<String> fieldNames = this.metadata.getFieldNames();
        final List<VariableElement> fields = this.metadata.getFields();
        final List<VariableElement> refFields = this.metadata.getRefFields();
        final List<String> refNames = this.metadata.getRefFieldNames();

        TypeName listOfClass = ParameterizedTypeName.get(listType, entityClass);

        List<MethodSpec> methodSpecs = new ArrayList<>();

        for (int i = 0; i < refNames.size(); i++) {

            String name = refNames.get(i);
            String refSetter = this.metadata.getSetter(name);
            final VariableElement variableElement = refFields.get(i);
            TypeMirror refE = variableElement.asType();

            RefLink refLink = variableElement.getAnnotation(RefLink.class);
            VariableElement onField = null;
            for (int j = 0; j < fieldNames.size(); j++) {
                if (refLink.on().equals(fieldNames.get(j))){
                    onField = fields.get(j);
                    break;
                }
            }

            //Utils.note("addWrapRefListMethod RefLink found original Field. class = " + this.className + ", refName=" + name + ", on=" + refLink.on());

            if (onField == null){
                Utils.error("RefLink can't found original Field. class = " + this.className + ", refName=" + name + ", on=" + refLink.on());
            }

            String onGetter = this.metadata.getGetter(refLink.on());
            TypeName onType = this.metadata.getBoxTypeName(onField.asType());
            if (onType == null){
                onType = TypeName.get(onField.asType());
            }

            //Utils.note("addWrapRefListMethod onType: " + onType.toString());

            // onType 仅允许是 int, long, String
            // 如果onType 是String

            MethodSpec.Builder wrap = MethodSpec.methodBuilder("wrapRef" + Utils.upperFirstChar(name))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(listOfClass, "list");

            if (typeStringName.equals(onType.toString())){
                genWrapListMethodForStringType(refSetter, refE, onGetter, onType, wrap);
            }else{
                genWrapListMethodForPrimitiveType(refSetter, refE, onGetter, onType, wrap);
            }

            final MethodSpec methodSpec = wrap.build();
            builder.addMethod(methodSpec);
            methodSpecs.add(methodSpec);

        }

        MethodSpec.Builder wrapAll = MethodSpec.methodBuilder("wrapRef")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(listOfClass, "list");

        wrapAll.beginControlFlow("if(list == null || list.size() == 0)");
        wrapAll.addStatement("return");
        wrapAll.endControlFlow();

        for (int i = 0; i < methodSpecs.size(); i++) {
            wrapAll.addStatement("$N(list)", methodSpecs.get(i).name);
        }

        builder.addMethod(wrapAll.build());

    }

    private void addWrapRefMethod(){

        final List<String> fieldNames = this.metadata.getFieldNames();
        final List<VariableElement> fields = this.metadata.getFields();
        final List<VariableElement> refFields = this.metadata.getRefFields();
        final List<String> refNames = this.metadata.getRefFieldNames();

        List<MethodSpec> methodSpecs = new ArrayList<>();

        for (int i = 0; i < refNames.size(); i++) {

            String name = refNames.get(i);
            String refSetter = this.metadata.getSetter(name);
            final VariableElement variableElement = refFields.get(i);
            TypeMirror refE = variableElement.asType();

            RefLink refLink = variableElement.getAnnotation(RefLink.class);
            VariableElement onField = null;
            for (int j = 0; j < fieldNames.size(); j++) {
                if (refLink.on().equals(fieldNames.get(j))){
                    onField = fields.get(j);
                    break;
                }
            }

            if (onField == null){
                Utils.error("RefLink can't found original Field. class = " + this.className + ", refName=" + name + ", on=" + refLink.on());
            }

            String onGetter = this.metadata.getGetter(refLink.on());
            TypeName onType = this.metadata.getBoxTypeName(onField.asType());
            if (onType == null){
                onType = TypeName.get(onField.asType());
            }

            //Utils.note("addWrapRefMethod onType: " + onType.toString());

            // onType 仅允许是 int, long, String
            // 如果onType 是String

            MethodSpec.Builder wrap = MethodSpec.methodBuilder("wrapRef" + Utils.upperFirstChar(name))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(entityClass, "o");

            if (typeStringName.equals(onType.toString())){
                genWrapMethodForStringType(refSetter, refE, onGetter, onType, wrap);
            }else{
                genWrapMethodForPrimitiveType(refSetter, refE, onGetter, onType, wrap);
            }

            final MethodSpec methodSpec = wrap.build();
            builder.addMethod(methodSpec);
            methodSpecs.add(methodSpec);

        }

        MethodSpec.Builder wrapAll = MethodSpec.methodBuilder("wrapRef")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(entityClass, "o");

        wrapAll.beginControlFlow("if(o == null)");
        wrapAll.addStatement("return");
        wrapAll.endControlFlow();

        for (int i = 0; i < methodSpecs.size(); i++) {
            wrapAll.addStatement("$N(o)", methodSpecs.get(i).name);
        }

        builder.addMethod(wrapAll.build());

    }

    /**
     *
     * 逗号分隔的字段
     *
     * @param refSetter
     * @param refE
     * @param onGetter
     * @param onType
     * @param wrap
     */
    private void genWrapMethodForStringType(String refSetter, TypeMirror refE, String onGetter, TypeName onType, MethodSpec.Builder wrap) {

        TypeName targetClassName = TypeName.get(refE);
        ClassName targetMapperClass = null;

        if (isListType(refE) || isSetType(refE)){
            ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(refE);
            targetClassName = className.typeArguments.get(0);
        }

        targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetClassName.toString()));
        TypeName listOfTarget = ParameterizedTypeName.get(listType, targetClassName);

        wrap.addStatement("$T ids = o.$N()", onType, onGetter);
        wrap.addStatement("$T refList = $T.instance.getsWithRef(ids)", listOfTarget, targetMapperClass);
        wrap.addStatement("o.$N(refList)", refSetter);

    }

    /**
     *
     * 原生类型(int, long)
     *
     * @param refSetter
     * @param refE
     * @param onGetter
     * @param onType
     * @param wrap
     */
    private void genWrapMethodForPrimitiveType(String refSetter, TypeMirror refE, String onGetter, TypeName onType, MethodSpec.Builder wrap) {

        TypeName targetClassName = TypeName.get(refE);
        ClassName targetMapperClass = null;

        if (isListType(refE) || isSetType(refE)){
            ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(refE);
            targetClassName = className.typeArguments.get(0);
        }

        targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetClassName.toString()));

        wrap.addStatement("$T id = o.$N()", onType, onGetter);
        wrap.beginControlFlow("if (id == null)");
        wrap.addStatement("return");
        wrap.endControlFlow();
        wrap.addStatement("$T refItem = $T.instance.getWithRef(id)", targetClassName, targetMapperClass);
        wrap.addStatement("o.$N(refItem)", refSetter);

    }

    /**
     *
     * 逗号分隔的字段
     *
     * @param refSetter
     * @param refE
     * @param onGetter
     * @param onType
     * @param wrap
     */
    private void genWrapListMethodForStringType(String refSetter, TypeMirror refE, String onGetter, TypeName onType, MethodSpec.Builder wrap) {

        TypeName targetClassName = TypeName.get(refE);
        ClassName targetMapperClass = null;

        if (isListType(refE) || isSetType(refE)){
            ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(refE);
            targetClassName = className.typeArguments.get(0);
        }

        targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetClassName.toString()));
        TypeName listOfTarget = ParameterizedTypeName.get(listType, targetClassName);

        wrap.beginControlFlow("for (int i = 0; i < list.size(); i++)");

        wrap.addStatement("$T item = list.get(i)", entityClass);
        wrap.addStatement("$T ids = item.$N()", onType, onGetter);
        wrap.addStatement("$T refList = $T.instance.getsWithRef(ids)", listOfTarget, targetMapperClass);
        wrap.addStatement("item.$N(refList)", refSetter);

        wrap.endControlFlow();

    }

    /**
     *
     * 原生类型(int, long)
     *
     * @param refSetter
     * @param refE
     * @param onGetter
     * @param onType
     * @param wrap
     */
    private void genWrapListMethodForPrimitiveType(String refSetter, TypeMirror refE, String onGetter, TypeName onType, MethodSpec.Builder wrap) {

        TypeName setOfOn = ParameterizedTypeName.get(setType, onType);
        TypeName hashSetOfOn = ParameterizedTypeName.get(hashSetType, onType);

        wrap.addStatement("$T ids = new $T()", setOfOn, hashSetOfOn);
        wrap.beginControlFlow("for (int i = 0; i < list.size(); i++)");
        wrap.addStatement("$T item = list.get(i)", entityClass);
        wrap.addStatement("ids.add(item.$N())", onGetter);
        wrap.endControlFlow();

        TypeName targetClassName = TypeName.get(refE);
        ClassName targetMapperClass = null;

        if (isListType(refE) || isSetType(refE)){
            ParameterizedTypeName className = (ParameterizedTypeName)TypeName.get(refE);
            targetClassName = className.typeArguments.get(0);
        }

        targetMapperClass = ClassName.bestGuess(Utils.getMapperClassName(targetClassName.toString()));
        TypeName listOfTarget = ParameterizedTypeName.get(listType, targetClassName);
        wrap.addStatement("$T refList = $T.instance.getsWithRef(ids)", listOfTarget, targetMapperClass);

        wrap.beginControlFlow("for (int i = 0; i < list.size(); i++)"); //1
        wrap.addStatement("$T item = list.get(i)", entityClass);

        wrap.beginControlFlow("for (int j = 0; j < refList.size(); j++)"); //2
        wrap.addStatement("$T targetItem = refList.get(j)", targetClassName);

        wrap.beginControlFlow("if (targetItem.getId() == item.$N())", onGetter); //3 每个实体一定有一个方法是getId()
        wrap.addStatement("item.$N(targetItem)", refSetter);
        wrap.addStatement("break");

        wrap.endControlFlow(); //3

        wrap.endControlFlow(); // 2

        wrap.endControlFlow(); //1
    }

}
