package com.argo.sqlite;

import com.argo.sqlite.annotations.Table;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * Created by user on 8/13/15.
 */
@SupportedAnnotationTypes({
        "com.argo.sqlite.annotations.Table"
})
public class SqliteEngineProcessor extends AbstractProcessor {


    Set<ClassMetaData> classesToValidate = new HashSet<ClassMetaData>();

    private boolean hasProcessedModules = false;

    @Override public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (hasProcessedModules) {
            return true;
        }

        Utils.initialize(processingEnv);

        Utils.note("SqliteEngineProcessor");

        // Create all proxy classes
        for (Element classElement : roundEnv.getElementsAnnotatedWith(Table.class)) {

            // Check the annotation was applied to a Class
            if (!classElement.getKind().equals(ElementKind.CLASS)) {
                Utils.error("The Table annotation can only be applied to classes", classElement);
            }

            ClassMetaData metadata = new ClassMetaData(processingEnv, (TypeElement) classElement);

            Utils.note("SqliteEngineProcessor Processing class " + metadata.getSimpleClassName());

            boolean success = metadata.generate();
            if (!success) {
                Utils.error("SqliteEngineProcessor Error Processing class " + metadata.getSimpleClassName());
                return true; // Abort processing by claiming all annotations
            }

            classesToValidate.add(metadata);

            SqliteMapperClassGenerator sourceCodeGenerator = new SqliteMapperClassGenerator(processingEnv, metadata);
            try {
                sourceCodeGenerator.generate();
            } catch (IOException e) {
                Utils.error(e.getMessage(), classElement);
            } catch (UnsupportedOperationException e) {
                Utils.error(e.getMessage(), classElement);
            }

            Utils.note("SqliteEngineProcessor Done Processing class " + metadata.getSimpleClassName());
        }

        SqliteModleInitGenerator initGenerator = new SqliteModleInitGenerator(processingEnv, classesToValidate);
        try {
            initGenerator.generate();
        } catch (IOException e) {
            e.printStackTrace();
        }

        hasProcessedModules = true;

        return true;
    }


}
