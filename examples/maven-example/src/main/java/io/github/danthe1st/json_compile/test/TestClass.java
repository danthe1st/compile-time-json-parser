package io.github.danthe1st.json_compile.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;

import io.github.danthe1st.json_compile.api.GenerateJSON;

@GenerateJSON
public class TestClass {
	
	public int someInt;
	
	private String someString;
	
	public String getSomeString() {
		return someString;
	}

	public void setSomeString(String someString) {
		this.someString = someString;
	}


	@Override
	public String toString() {
		return "TestClass [someInt=" + someInt + ", someString=" + someString + "]";
	}

	public static void main(String[] args) throws IOException {
		String json= String.join("", Files.readAllLines(Path.of("testClass.json")));
		TestClass obj = TestClassJSONLoader.fromJSON(json);
		System.out.println(obj);
	}
}
