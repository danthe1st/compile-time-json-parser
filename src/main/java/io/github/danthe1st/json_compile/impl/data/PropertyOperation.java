package io.github.danthe1st.json_compile.impl.data;

import io.github.danthe1st.json_compile.impl.JSONCreator;

import javax.lang.model.type.TypeMirror;

public class PropertyOperation extends JSONOperation{

    public PropertyOperation(String attributeName, TypeMirror type) {
        super(attributeName, type);
    }

    @Override
    public String getAccessor(String objectName) {
        return objectName+".get" + JSONCreator.capitalizeFirst(getAttributeName()) + "()";
    }

    @Override
    public String getMutator(String objectName,String newValue) {
        return objectName+".set" + JSONCreator.capitalizeFirst(getAttributeName()) + "("+newValue+")";
    }
}
