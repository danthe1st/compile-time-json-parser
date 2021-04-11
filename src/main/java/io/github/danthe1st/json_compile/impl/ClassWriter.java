package io.github.danthe1st.json_compile.impl;

import java.io.IOException;
import java.io.Writer;

import io.github.danthe1st.json_compile.impl.data.VariableDefinition;

public class ClassWriter implements AutoCloseable{
	private final Writer writer;
	private String className;
	private int indentation=0;
	
	public ClassWriter(Writer writer) {
		this.writer=writer;
	}

	//region class definition
	public void packageDeclaration(String packageName) throws IOException {
		writer.write("package ");
		writer.write(packageName);
		endStatement();
	}

	public void addImport(String name) throws IOException {
		writer.write("import ");
		writer.write(name);
		endStatement();
	}
	
	public void beginClass(String className) throws IOException {
		this.className=className;
		writer.write("public class ");
		writer.write(className);
		beginBlock();
	}
	
	public void endClass() throws IOException {
		endBlock();
	}
	//endregion

	//region attributes
	public void addAttribute(VariableDefinition variable) throws IOException {
		writer.write("private ");
		addVariable(variable);
	}
	//endregion
	
	//region constructors
	public void addConstructor(VariableDefinition... variables) throws IOException {
		beginConstructorDefinition();
		writeParams(variables);
		beginConstructorBody();
		for (VariableDefinition variable: variables) {
			createAttributeAssignment(variable.getName(), variable.getName());
		}
		endConstructor();
	}
	
	private void beginConstructorDefinition() throws IOException {
		writer.write("public ");
		writer.write(className);
	}
	
	private void endConstructor() throws IOException {
		endBlock();
	}
	
	
	
	private void beginConstructorBody() throws IOException {
		beginBlock();
	}
	
	//endregion
	
	//region methods
	
	//public static method
	public void beginMethod(String name,String returnType,VariableDefinition... parameters) throws IOException {
		writer.write("public static ");
		writer.write(returnType);
		writer.write(" ");
		writer.write(name);
		writeParams(parameters);
		beginBlock();
	}
	
	
	public void endMethod() throws IOException {
		endBlock();
	}
	
	
	//endregion
	
	private void writeParams(VariableDefinition[] parameters) throws IOException {
		writer.write("(");
		for (int i = 0; i < parameters.length; i++) {
			VariableDefinition variable = parameters[i];
			addParameter(variable);
			if(i<parameters.length-1) {
				writer.write(",");
			}
		}
		writer.write(")");
	}
	
	private void addParameter(VariableDefinition variable) throws IOException {
		addVariableRaw(variable);
	}
	
	private void createAttributeAssignment(String name,String value) throws IOException {
		writer.write("this.");
		writer.write(name);
		writer.write("=");
		writer.write(value);
		endStatement();
	}
	
	public void addVariable(VariableDefinition variable,String defaultValue) throws IOException {
		addVariableRaw(variable);
		writer.write("=");
		writer.write(defaultValue);
		endStatement();
	}
	public void addVariable(VariableDefinition variable) throws IOException {
		addVariableRaw(variable);
		endStatement();
	}
	private void addVariableRaw(VariableDefinition variable) throws IOException {
		writer.write(variable.getType());
		writer.write(" ");
		writer.write(variable.getName());
	}
	
	//region statements
	
	public void addAssignment(String name,String value) throws IOException {
		writer.write(name);
		writer.write("=");
		writer.write(value);
		endStatement();
	}
	
	public void addReturn(String toReturn) throws IOException {
		writer.write("return ");
		writer.write(toReturn);
		endStatement();
	}
	
	public void addMethodCall(String objectName, String methodName, String... paramNames) throws IOException {
		writer.write(objectName);
		writer.write(".");
		writer.write(methodName);
		writer.write("(");
		writer.write(String.join(",", paramNames));
		writer.write(")");
		endStatement();
	}

	public void beginIf(String condition) throws IOException {
		writer.write("if (");
		writer.write(condition);
		writer.write(") ");
		beginBlock();
	}

	public void endIf() throws IOException {
		endBlock();
	}

	public void beginSimpleFor(String init,String condition,String advancement) throws IOException{
		writer.write("for (");
		writer.write(init);
		writer.write("; ");
		writer.write(condition);
		writer.write("; ");
		writer.write(advancement);
		writer.write(")");
		beginBlock();
	}

	public void endFor() throws IOException{
		endBlock();
	}
	
	private void endStatement() throws IOException {
		writer.write(";");
		nextLine();
	}
	//endregion
	
	public void beginBlock() throws IOException {
		writer.write("{");
		indentation++;
		nextLine();
	}
	public void endBlock() throws IOException {
		writer.write("}");
		indentation--;
		nextLine();
	}
	private void nextLine() throws IOException {
		writer.write(System.lineSeparator());
		indent();
	}
	private void indent() throws IOException {
		writer.write("\t".repeat(indentation));
	}
	
	@Override
	public void close() throws IOException {
		writer.close();	
	}

	
}
