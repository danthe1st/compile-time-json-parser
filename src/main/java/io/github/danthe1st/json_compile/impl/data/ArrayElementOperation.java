package io.github.danthe1st.json_compile.impl.data;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;

public class ArrayElementOperation extends JSONOperation{
    public ArrayElementOperation(String attributeName, TypeMirror type) {
        super(attributeName, OperationType.ARRAY_ELEMENT, type);
    }

    @Override
    public String getAccessor(String objectName) {
        return getAttributeName();
    }

    @Override
    public String getMutator(String objectName,String newValue) {
        return getAccessor(objectName)+"="+newValue;
    }

    @Override
    public boolean isChildType() {
        return true;
    }

    @Override
    public String getJSONAccessName() {
        String name=getAttributeName();
        return name.substring(name.indexOf('[')+1,name.indexOf(']'));
    }
}
