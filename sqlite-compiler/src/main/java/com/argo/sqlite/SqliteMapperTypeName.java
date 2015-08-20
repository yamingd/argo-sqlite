package com.argo.sqlite;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeWriter;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.squareup.javapoet.Util.checkArgument;
import static com.squareup.javapoet.Util.checkNotNull;

/**
 * Created by user on 8/14/15.
 */
public class SqliteMapperTypeName extends TypeName {

    public final ClassName rawType;
    public final List<TypeName> typeArguments;

    public SqliteMapperTypeName(ClassName rawType, List<TypeName> typeArguments) {
        this.rawType = checkNotNull(rawType, "rawType == null");
        this.typeArguments = Util.immutableList(typeArguments);

        checkArgument(!this.typeArguments.isEmpty(), "no type arguments: %s", rawType);

    }

    @Override public boolean equals(Object o) {
        return o instanceof SqliteMapperTypeName
                && ((SqliteMapperTypeName) o).rawType.equals(rawType)
                && ((SqliteMapperTypeName) o).typeArguments.equals(typeArguments);
    }

    @Override public int hashCode() {
        return rawType.hashCode() + 31 * typeArguments.hashCode();
    }

    @Override
    public CodeWriter emit(CodeWriter out) throws IOException {
        rawType.emit(out);
        out.emitAndIndent("<");
        boolean firstParameter = true;
        for (TypeName parameter : typeArguments) {
            if (!firstParameter) out.emitAndIndent(", ");
            parameter.emit(out);
            firstParameter = false;
        }
        return out.emitAndIndent(">");
    }

    public static SqliteMapperTypeName get(ClassName rawType, TypeName... typeArguments) {
        return new SqliteMapperTypeName(rawType, Arrays.asList(typeArguments));
    }
}
