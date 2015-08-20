package com.argo.sqlite;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeWriter;
import com.squareup.javapoet.TypeName;

import java.io.IOException;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * Created by user on 8/14/15.
 */
public class EntityClassTypeName extends TypeName {

    public final ClassName rawType;

    public EntityClassTypeName(ClassName rawType) {
        this.rawType = checkNotNull(rawType, "rawType == null");

    }

    @Override public boolean equals(Object o) {
        return o instanceof EntityClassTypeName
                && ((EntityClassTypeName) o).rawType.equals(rawType);
    }

    @Override public int hashCode() {
        return rawType.hashCode();
    }

    @Override
    public CodeWriter emit(CodeWriter out) throws IOException {
        out.emitAndIndent("Class<");
        rawType.emit(out);
        return out.emitAndIndent(">");
    }

    public static EntityClassTypeName get(ClassName rawType) {
        return new EntityClassTypeName(rawType);
    }
}
