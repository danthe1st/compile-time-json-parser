package io.github.danthe1st.json_compile.impl.data;

import javax.lang.model.type.TypeMirror;

public class FieldOperation extends JSONOperation{

    public FieldOperation(String attributeName, TypeMirror type) {
        super(attributeName, type);
    }

    @Override
    public String getAccessor(String objectName) {
        return objectName+"."+getAttributeName();
    }

    @Override
    public String getMutator(String objectName,String newValue) {
        return getAccessor(objectName)+"="+newValue;
    }
}
