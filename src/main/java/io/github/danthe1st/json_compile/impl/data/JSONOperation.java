package io.github.danthe1st.json_compile.impl.data;

import javax.lang.model.type.TypeMirror;

public class JSONOperation {
	private final String attributeName;
	private final OperationType opType;
	private final TypeMirror type;

	public JSONOperation(String attributeName, OperationType opType, TypeMirror type) {
		this.attributeName = attributeName;
		this.opType = opType;
		this.type = type;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public OperationType getOpType() {
		return opType;
	}
	
	public TypeMirror getType() {
		return type;
	}

	public enum OperationType {
		FIELD, ARRAY_ELEMENT, PROPERTY
	}
}
