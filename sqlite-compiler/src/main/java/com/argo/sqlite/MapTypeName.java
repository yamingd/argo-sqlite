package com.argo.sqlite;

import com.squareup.javapoet.CodeWriter;
import com.squareup.javapoet.TypeName;

import java.io.IOException;

import static com.squareup.javapoet.Util.checkNotNull;

/**
 * Created by user on 8/14/15.
 */
public class MapTypeName extends TypeName {

    public final TypeName varType;
    public final TypeName keyType;
    public final TypeName valueType;

    public MapTypeName(TypeName varType, TypeName keyType, TypeName valueType) {
        this.varType = varType;
        this.keyType = checkNotNull(keyType, "keyType == null");
        this.valueType = checkNotNull(valueType, "valueType == null");
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof MapTypeName)){
            return false;
        }
        MapTypeName o2 = (MapTypeName)o;

        return o2.varType.equals(varType)
                && o2.keyType.equals(keyType)
                && o2.valueType.equals(valueType);
    }

    @Override public int hashCode() {
        return 31 * keyType.hashCode() * valueType.hashCode();
    }

    @Override public CodeWriter emit(CodeWriter out) throws IOException {
        return out.emit("$T<$T, $T>", varType, keyType, valueType);
    }

}
