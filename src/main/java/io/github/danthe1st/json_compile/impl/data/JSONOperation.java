package io.github.danthe1st.json_compile.impl.data;

import javax.lang.model.type.TypeMirror;

public abstract class JSONOperation {
	private final String attributeName;
	private final OperationType opType;
	private final TypeMirror type;

	protected JSONOperation(String attributeName, OperationType opType, TypeMirror type) {
		this.attributeName = attributeName;
		this.opType = opType;
		this.type = type;
	}

	public String getAttributeName() {
		return attributeName;
	}

	@Deprecated
	public OperationType getOpType() {
		return opType;
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

	@Deprecated//TODO remove
	public enum OperationType {
		FIELD, ARRAY_ELEMENT, PROPERTY
	}
}
