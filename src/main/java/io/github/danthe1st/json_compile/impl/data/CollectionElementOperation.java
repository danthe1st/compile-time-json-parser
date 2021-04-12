package io.github.danthe1st.json_compile.impl.data;

import javax.lang.model.type.TypeMirror;

public class CollectionElementOperation extends JSONOperation{
    private final String collectionName;
    public CollectionElementOperation(String collectionName,String elementName, TypeMirror type) {
        super(elementName, OperationType.ARRAY_ELEMENT, type);
        this.collectionName=collectionName;
    }

    @Override
    public String getAccessor(String objectName) {
        return getAttributeName();
    }

    @Override
    public String getMutator(String objectName,String newValue) {
        return collectionName+".add("+newValue+")";
    }

    @Override
    public boolean isChildType() {
        return true;
    }

    @Override
    public String getJSONAccessName() {
        return getAttributeName();//TODO use counter here
    }
}
