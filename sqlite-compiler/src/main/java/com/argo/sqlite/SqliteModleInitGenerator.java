package com.argo.sqlite;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;

/**
 * Created by user on 8/15/15.
 */
public class SqliteModleInitGenerator {

    private ProcessingEnvironment processingEnvironment;
    private Set<ClassMetaData> mappers;
    private TypeSpec.Builder builder;

    public SqliteModleInitGenerator(ProcessingEnvironment processingEnvironment, Set<ClassMetaData> mappers) {
        this.processingEnvironment = processingEnvironment;
        this.mappers = mappers;
    }

    public void generate() throws IOException, UnsupportedOperationException {
        String packageName = "";
        String className = "ModelInit";

        Iterator<ClassMetaData> iterator = this.mappers.iterator();
        while (iterator.hasNext()){
            ClassMetaData classMetaData = iterator.next();
            packageName = classMetaData.getPackageName();
            break;
        }

        int pos = packageName.lastIndexOf(".");
        //Utils.note("packageName pos: " + pos);
        packageName = packageName.substring(0, pos);

        String fullName = packageName + "." + className;

        builder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC).addModifiers(Modifier.FINAL);

        this.addStaticFields();
        this.addStaticInitCodes();
        this.addPrepareMethod();
        this.addResetMethod();

        JavaFile javaFile = JavaFile.builder(packageName, builder.build())
                .build();

        JavaFileObject sourceFile = processingEnvironment.getFiler().createSourceFile(fullName);
        final BufferedWriter bufferedWriter = new BufferedWriter(sourceFile.openWriter());
        javaFile.writeTo(bufferedWriter);
        bufferedWriter.flush();
        bufferedWriter.close();

    }

    private void addStaticFields(){

        Iterator<ClassMetaData> iterator = this.mappers.iterator();
        while (iterator.hasNext()){

            ClassMetaData classMetaData = iterator.next();
            String mapperClassName = Utils.getMapperClassName(classMetaData.getSimpleClassName());
            ClassName mapperClass = ClassName.bestGuess(classMetaData.getPackageName() + "." + mapperClassName);

            builder.addField(mapperClass, Utils.lowerFirstChar(mapperClassName), Modifier.PUBLIC, Modifier.STATIC);

        }

    }

    private void addStaticInitCodes(){

        Iterator<ClassMetaData> iterator = this.mappers.iterator();

        CodeBlock.Builder block = CodeBlock.builder();

        while (iterator.hasNext()){

            ClassMetaData classMetaData = iterator.next();
            String mapperClassName = Utils.getMapperClassName(classMetaData.getSimpleClassName());
            ClassName mapperClass = ClassName.bestGuess(mapperClassName);

            block.addStatement("$N = new $T()", Utils.lowerFirstChar(mapperClassName), mapperClass);

        }

        builder.addStaticBlock(block.build());

    }

    private void addPrepareMethod(){

        MethodSpec.Builder prepare = MethodSpec.methodBuilder("prepare")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC);

        Iterator<ClassMetaData> iterator = this.mappers.iterator();
        while (iterator.hasNext()){

            ClassMetaData classMetaData = iterator.next();
            String mapperClassName = Utils.getMapperClassName(classMetaData.getSimpleClassName());

            prepare.addStatement("$N.prepare()", Utils.lowerFirstChar(mapperClassName));

        }

        builder.addMethod(prepare.build());

    }

    private void addResetMethod(){

        MethodSpec.Builder reset = MethodSpec.methodBuilder("reset")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC);

        Iterator<ClassMetaData> iterator = this.mappers.iterator();
        while (iterator.hasNext()){

            ClassMetaData classMetaData = iterator.next();
            String mapperClassName = Utils.getMapperClassName(classMetaData.getSimpleClassName());

            reset.addStatement("$N.resetStatement()", Utils.lowerFirstChar(mapperClassName));

        }

        builder.addMethod(reset.build());

    }
}
