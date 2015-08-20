package com.argo.sqlite;

import com.argo.sqlite.annotations.Column;
import com.argo.sqlite.annotations.RefLink;
import com.argo.sqlite.annotations.Table;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.lang.model.util.Types;


/**
 * Utility class for holding metadata for SqliteMapper classes.
 */
public class ClassMetaData {

    private final TypeElement classType; // Reference to model class.
    private Table tableAnnotation;
    private String className; // Model class simple name.
    private String packageName; // package name for model class.
    private boolean hasDefaultConstructor; // True if model has a public no-arg constructor.
    private VariableElement primaryKey; // Reference to field used as primary key, if any.

    private List<VariableElement> fields = new ArrayList<VariableElement>(); // List of all fields in the class except those @Ignored.
    private List<String> fieldNames = new ArrayList<String>();

    private List<VariableElement> refFields = new ArrayList<>();
    private List<String> refFieldNames = new ArrayList<String>();

    private Set<String> expectedGetters = new HashSet<String>(); // Set of fieldnames that are expected to have a getter
    private Set<String> expectedSetters = new HashSet<String>(); // Set of fieldnames that are expected to have a setter
    private Set<ExecutableElement> methods = new HashSet<ExecutableElement>(); // List of all methods in the model class
    private Map<String, String> getters = new HashMap<String, String>(); // Map between fieldnames and their getters
    private Map<String, String> setters = new HashMap<String, String>(); // Map between fieldname and their setters

    private final List<PrimitiveType> validPrimaryKeyTypes;
    private final Types typeUtils;
    //private DeclaredType realmList;

    public ClassMetaData(ProcessingEnvironment env, TypeElement clazz) {
        this.classType = clazz;
        this.className = clazz.getSimpleName().toString();
        this.tableAnnotation = clazz.getAnnotation(Table.class);

        typeUtils = env.getTypeUtils();
        //realmList = typeUtils.getDeclaredType(env.getElementUtils().getTypeElement("io.realm.RealmList"), typeUtils.getWildcardType(null, null));
        validPrimaryKeyTypes = Arrays.asList(
                typeUtils.getPrimitiveType(TypeKind.SHORT),
                typeUtils.getPrimitiveType(TypeKind.INT),
                typeUtils.getPrimitiveType(TypeKind.LONG)
        );
    }

    /**
     * Build the meta data structures for this class. Any errors or messages will be
     * posted on the provided Messager.
     *
     * @return True if meta data was correctly created and processing can continue, false otherwise.
     */
    public boolean generate() {

        // Get the package of the class
        Element enclosingElement = classType.getEnclosingElement();
        if (!enclosingElement.getKind().equals(ElementKind.PACKAGE)) {
            Utils.error("The Table annotation does not support nested classes", classType);
            return false;
        }


        PackageElement packageElement = (PackageElement) enclosingElement;
        packageName = packageElement.getQualifiedName().toString();

        if (!categorizeClassElements()) return false;

        //Utils.note("Getters: " + expectedGetters);

        this.mappingMethods();

        if (!checkDefaultConstructor()) return false;
        if (!checkRequiredGetters()) return false;
        if (!checkRequireSetters()) return false;

        return true; // Meta data was successfully generated
    }

    private void mappingMethods() {

        for (ExecutableElement executableElement : methods) {

            String methodName = executableElement.getSimpleName().toString();

            // Check the modifiers of the method
            Set<Modifier> modifiers = executableElement.getModifiers();
            if (modifiers.contains(Modifier.STATIC)) {
                continue; // We're cool with static methods. Move along!
            }

            // Check that getters and setters are valid
            if (methodName.startsWith("get") || methodName.startsWith("is")) {
                checkGetterMethod(methodName);
            } else if (methodName.startsWith("set")) {
                checkSetterMethod(methodName);
            }

        }

    }

    // Report any setters that are missing
    private boolean checkRequireSetters() {
        for (String expectedSetter : expectedSetters) {
            Utils.error("No setter found for field " + expectedSetter);
        }
        return expectedSetters.size() == 0;
    }

    // Report any getters that are missing
    private boolean checkRequiredGetters() {
        for (String expectedGetter : expectedGetters) {
            Utils.error("No getter found for field " + expectedGetter);
        }
        return expectedGetters.size() == 0;
    }

    // Verify that a setter is used to set a field in the model class.
    // Note: This is done heuristically by comparing the name of setter with the name of the field.
    // Annotation processors does not allow us to inspect individual statements.
    private boolean checkSetterMethod(String methodName) {
        boolean found = false;

        String methodMinusSet = methodName.substring(3);
        String methodMinusSetCapitalised = Utils.lowerFirstChar(methodMinusSet);
        String methodMenusSetPlusIs = "is" + methodMinusSet;

        if (fieldNames.contains(methodMinusSet) || refFieldNames.contains(methodMinusSet)) { // mPerson -> setmPerson
            expectedSetters.remove(methodMinusSet);
            setters.put(methodMinusSet, methodName);
            found = true;
        } else if (fieldNames.contains(methodMinusSetCapitalised) || refFieldNames.contains(methodMinusSetCapitalised)) { // person -> setPerson
            expectedSetters.remove(methodMinusSetCapitalised);
            setters.put(methodMinusSetCapitalised, methodName);
            found = true;
        } else if (fieldNames.contains(methodMenusSetPlusIs) || refFieldNames.contains(methodMenusSetPlusIs)) { // isReady -> setReady
            expectedSetters.remove(methodMenusSetPlusIs);
            setters.put(methodMenusSetPlusIs, methodName);
            found = true;
        }

        return found;
    }

    private boolean checkGetterMethod(String methodName) {
        boolean found = false;

        if (methodName.startsWith("is")) {
            String methodMinusIs = methodName.substring(2);
            String methodMinusIsCapitalised = Utils.lowerFirstChar(methodMinusIs);
            if (fieldNames.contains(methodName) || refFieldNames.contains(methodName)) { // isDone -> isDone
                expectedGetters.remove(methodName);
                getters.put(methodName, methodName);
                found = true;
            } else if (fieldNames.contains(methodMinusIs) || refFieldNames.contains(methodMinusIs)) {  // mDone -> ismDone
                expectedGetters.remove(methodMinusIs);
                getters.put(methodMinusIs, methodName);
                found = true;
            } else if (fieldNames.contains(methodMinusIsCapitalised) || refFieldNames.contains(methodMinusIsCapitalised)) { // done -> isDone
                expectedGetters.remove(methodMinusIsCapitalised);
                getters.put(methodMinusIsCapitalised, methodName);
                found = true;
            }
        }

        if (!found && methodName.startsWith("get")) {
            String methodMinusGet = methodName.substring(3);
            String methodMinusGetCapitalised = Utils.lowerFirstChar(methodMinusGet);

            if (fieldNames.contains(methodMinusGet) || refFieldNames.contains(methodMinusGet)) { // mPerson -> getmPerson
                expectedGetters.remove(methodMinusGet);
                getters.put(methodMinusGet, methodName);
                found = true;
            } else if (fieldNames.contains(methodMinusGetCapitalised) || refFieldNames.contains(methodMinusGetCapitalised)) { // person -> getPerson
                expectedGetters.remove(methodMinusGetCapitalised);
                getters.put(methodMinusGetCapitalised, methodName);
                found = true;
            }
        }

        return found;
    }


    // Report if the default constructor is missing
    private boolean checkDefaultConstructor() {
        if (!hasDefaultConstructor) {
            Utils.error("A default public constructor with no argument must be declared if a custom constructor is declared.");
            return false;
        } else {
            return true;
        }
    }

    // Iterate through all class elements and add them to the appropriate internal data structures.
    // Returns true if all elements could be false if elements could not be categorized,
    private boolean categorizeClassElements() {
        for (Element element : classType.getEnclosedElements()) {
            ElementKind elementKind = element.getKind();

            if (elementKind.equals(ElementKind.FIELD)) {
                VariableElement variableElement = (VariableElement) element;
                String fieldName = variableElement.getSimpleName().toString();

                Set<Modifier> modifiers = variableElement.getModifiers();
                if (modifiers.contains(Modifier.STATIC)) {
                    continue; // completely ignore any static fields
                }

                final Column column = variableElement.getAnnotation(Column.class);
                final RefLink refLink = variableElement.getAnnotation(RefLink.class);

                if (column == null && refLink == null){
                    continue;
                }

                if (!variableElement.getModifiers().contains(Modifier.PRIVATE) && (column != null || refLink != null)) {
                    Utils.error("The fields of the model must be private", variableElement);
                    return false;
                }

                if (column != null) {
                    // The field has the @column annotation.
                    fields.add(variableElement);

                    expectedGetters.add(fieldName);
                    expectedSetters.add(fieldName);

                    if (column.pk()){
                        // Primary Key
                        primaryKey = variableElement;
                    }
                }


                if (refLink != null) {
                    // The field has the @refLink annotation.
                    refFields.add(variableElement);

                    expectedGetters.add(fieldName);
                    expectedSetters.add(fieldName);

                    DeclaredType declaredType = (DeclaredType)variableElement.asType();

                    /**
                     *

                     Note: Found Ref Type. com.inno.k12.model.catalog.TSCity
                     Note: Found Ref asElement. com.inno.k12.model.catalog.TSCity
                     Note: Found Ref asElement Type. com.inno.k12.model.catalog.TSCity
                     Note: Found Ref TypeArguments.
                     Note: Found Ref Es. city

                     Note: Found Ref Type. java.util.List<com.inno.k12.model.catalog.TSCity>
                     Note: Found Ref asElement. java.util.List
                     Note: Found Ref asElement Type. java.util.List<E>
                     Note: Found Ref TypeArguments. com.inno.k12.model.catalog.TSCity
                     Note: Found Ref Es. subCities

                     */

//                    Utils.note("Found Ref Type. " + declaredType);
//                    Utils.note("Found Ref asElement. " + declaredType.asElement());
//                    Utils.note("Found Ref asElement Type. " + declaredType.asElement().asType());
//                    Utils.note("Found Ref TypeArguments. " + declaredType.getTypeArguments());
//                    Utils.note("Found Ref Es. " + element);

                }


            } else if (elementKind.equals(ElementKind.CONSTRUCTOR)) {
                hasDefaultConstructor =  hasDefaultConstructor || Utils.isDefaultConstructor(element);

            } else if (elementKind.equals(ElementKind.METHOD)) {
                ExecutableElement executableElement = (ExecutableElement) element;
                methods.add(executableElement);
            }
        }

        for (VariableElement field : fields) {
            fieldNames.add(field.getSimpleName().toString());
        }

        if (fields.size() == 0) {
            Utils.error(className + " must contain at least 1 persistable field");
        }

        for (VariableElement field : refFields) {
            refFieldNames.add(field.getSimpleName().toString());
        }

        return true;
    }

    public Table getTableAnnotation() {
        return tableAnnotation;
    }

    public String getSimpleClassName() {
        return className;
    }

    public String getFullyQualifiedClassName() {
        return packageName + "." + className;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public List<String> getRefFieldNames() {
        return refFieldNames;
    }

    public List<VariableElement> getFields() {
        return fields;
    }

    public List<VariableElement> getRefFields() {
        return refFields;
    }

    public String getGetter(String fieldName) {
        return getters.get(fieldName);
    }

    public String getSetter(String fieldName) {
        return setters.get(fieldName);
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean hasPrimaryKey() {
        return primaryKey != null;
    }

    public VariableElement getPrimaryKey() {
        return primaryKey;
    }

    public String getFieldTypeName(int i){
        VariableElement element = this.fields.get(i);
        return getFieldTypeName(element);
    }

    public String getFieldTypeName(VariableElement element) {
        final TypeMirror typeMirror = element.asType();
        Utils.note("getFieldTypeName: " + element + ", " + typeMirror);

        final String typeName = typeMirror.accept(new SimpleTypeVisitor7<String, Void>() {
            @Override public String visitPrimitive(PrimitiveType t, Void p) {
                return t.getKind().name().toLowerCase();
            }

            @Override
            public String visitDeclared(DeclaredType t, Void aVoid) {
                TypeElement e = (TypeElement)t.asElement();
                return e.getSimpleName().toString();
            }

            @Override
            public String visitArray(ArrayType t, Void aVoid) {
                return typeMirror.toString();
            }

        }, null);

        if (typeName == null){
            return typeMirror.toString();
        }

        return typeName;
    }

    public TypeName getPrimaryKeyTypeName() {
        final TypeMirror typeMirror = this.getPrimaryKey().asType();
        //Utils.note("typeMirror: " + typeMirror);
        return getBoxTypeName(typeMirror);
    }

    public TypeName getBoxTypeName(final TypeMirror typeMirror) {
        final TypeName typeName = typeMirror.accept(new SimpleTypeVisitor7<TypeName, Void>() {
            @Override public TypeName visitPrimitive(PrimitiveType t, Void p) {
                switch (t.getKind()) {
                    case BOOLEAN:
                        return TypeName.get(Boolean.class);
                    case BYTE:
                        return TypeName.get(Byte.class);
                    case SHORT:
                        return TypeName.get(Short.class);
                    case INT:
                        return TypeName.get(Integer.class);
                    case LONG:
                        return TypeName.get(Long.class);
                    case CHAR:
                        return TypeName.get(Character.class);
                    case FLOAT:
                        return TypeName.get(Float.class);
                    case DOUBLE:
                        return TypeName.get(Double.class);
                    default:
                        return null;
                }
            }


        }, null);

        //Utils.note("getBoxTypeName: " + typeName);
        return typeName;
    }

    public String getPrimaryKeyGetter() {
        return getters.get(primaryKey.getSimpleName().toString());
    }

    private boolean isValidPrimaryKeyType(TypeMirror type) {
        for (TypeMirror validType : validPrimaryKeyTypes) {
            if (typeUtils.isAssignable(type, validType)) {
                return true;
            }
        }
        return false;
    }

    public TypeElement getClassType() {
        return classType;
    }
}

