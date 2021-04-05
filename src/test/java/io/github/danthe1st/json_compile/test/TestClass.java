package io.github.danthe1st.json_compile.test;

import io.github.danthe1st.json_compile.api.GenerateJSON;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@GenerateJSON
public class TestClass {
	private int privVal;
	public String pubVal;
	
	public int getProp() {
		return privVal;
	}
	public void setProp(int i) {
		this.privVal=i;
	}

	@Override
	public String toString() {
		return "TestClass{" +
				"privVal=" + privVal +
				", pubVal='" + pubVal + '\'' +
				'}';
	}

	public static void main(String[] args) throws IOException {
		String json= String.join("", Files.readAllLines(Path.of("testClass.json")));
		TestClass obj = TestClassJSONLoader.fromJSON(new JSONObject(json));
		System.out.println(obj);
	}
}
