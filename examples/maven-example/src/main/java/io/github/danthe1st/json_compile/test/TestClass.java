package io.github.danthe1st.json_compile.test;

import io.github.danthe1st.json_compile.api.GenerateJSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@GenerateJSON
public class TestClass {
	
	public int someInt;
	
	private String someString;

	private int[][] someArray;
	
	public String getSomeString() {
		return someString;
	}

	public void setSomeString(String someString) {
		this.someString = someString;
	}

	public int[][] getSomeArray() {
		return someArray;
	}

	public void setSomeArray(int[][] someArray) {
		this.someArray = someArray;
	}

	@Override
	public String toString() {
		return "TestClass{" +
				"someInt=" + someInt +
				", someString='" + someString + '\'' +
				", someArray=" + Arrays.deepToString(someArray) +
				'}';
	}

	public static void main(String[] args) throws IOException {
		String json= String.join("", Files.readAllLines(Path.of("testClass.json")));
		TestClass obj = TestClassJSONLoader.fromJSON(json);
		System.out.println(obj);
		TestClass testObj=new TestClass();
		testObj.setSomeString("test");
		testObj.someInt=12345;
		testObj.someArray=new int[][]{{1,2,3},{},null,{1,2,3,4,5,6}};
		System.out.println(TestClassJSONLoader.toJSON(testObj));
	}
}
