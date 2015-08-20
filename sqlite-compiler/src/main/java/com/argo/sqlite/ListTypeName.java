package com.argo.sqlite;

import com.squareup.javapoet.CodeWriter;
import com.squareup.javapoet.TypeName;

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

import javax.lang.model.type.ArrayType;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * Created by user on 8/14/15.
 */
public class ListTypeName extends TypeName {

    public final TypeName componentType;

    private ListTypeName(TypeName componentType) {
        this.componentType = checkNotNull(componentType, "rawType == null");
    }

    @Override public boolean equals(Object o) {
        return o instanceof ListTypeName
                && ((ListTypeName) o).componentType.equals(componentType);
    }

    @Override public int hashCode() {
        return 31 * componentType.hashCode();
    }

    @Override public CodeWriter emit(CodeWriter out) throws IOException {
        return out.emit("List<$T>", componentType);
    }

    /** Returns an array type whose elements are all instances of {@code componentType}. */
    public static ListTypeName of(TypeName componentType) {
        return new ListTypeName(componentType);
    }

    /** Returns an array type whose elements are all instances of {@code componentType}. */
    public static ListTypeName of(Type componentType) {
        return of(TypeName.get(componentType));
    }

    /** Returns an array type equivalent to {@code mirror}. */
    public static ListTypeName get(ArrayType mirror) {
        return new ListTypeName(get(mirror.getComponentType()));
    }

    /** Returns an array type equivalent to {@code type}. */
    public static ListTypeName get(GenericArrayType type) {
        return ListTypeName.of(get(type.getGenericComponentType()));
    }
}
