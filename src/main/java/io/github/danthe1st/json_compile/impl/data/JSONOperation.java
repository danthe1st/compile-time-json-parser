package io.github.danthe1st.json_compile.impl.data;

import javax.lang.model.type.TypeMirror;

public abstract class JSONOperation {
	private final String attributeName;
	private final TypeMirror type;

	protected JSONOperation(String attributeName, TypeMirror type) {
		this.attributeName = attributeName;
		this.type = type;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public TypeMirror getType() {
		return type;
	}

	public abstract String getAccessor(String objectName);

	public abstract String getMutator(String objectName,String newValue);

	public String getJSONAccessName(){
		return '"'+attributeName+'"';
	}

	public boolean isChildType(){
		return false;
	}
}
