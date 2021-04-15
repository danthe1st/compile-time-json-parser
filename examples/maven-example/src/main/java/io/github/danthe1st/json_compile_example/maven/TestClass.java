package io.github.danthe1st.json_compile_example.maven;

import io.github.danthe1st.json_compile.api.GenerateJSON;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@GenerateJSON
public class TestClass {
	private int privVal;//private values without getters/setters-->ignored
	public String pubVal;

	public int[][] data={{1,2,3},{},{1,2,3,4,5,6}};//private values without getters/setters-->used

	private ReferencedClass otherObject=new ReferencedClass();

	private List<ReferencedClass[]> list=new ArrayList<>();

	private TestEnum someEnum;
	
	public int getProp() {
		return privVal;
	}
	public void setProp(int i) {
		this.privVal=i;
	}

	public ReferencedClass getOtherObject() {
		return otherObject;
	}

	public void setOtherObject(ReferencedClass otherObject) {
		this.otherObject = otherObject;
	}

	public List <ReferencedClass[]> getList() {
		return list;
	}

	public void setList(List <ReferencedClass[]> list) {
		this.list = list;
	}

	public TestEnum getSomeEnum() {
		return someEnum;
	}

	public void setSomeEnum(TestEnum someEnum) {
		this.someEnum = someEnum;
	}

	@Override
	public String toString() {
		return "TestClass{" +
				"privVal=" + privVal +
				", pubVal='" + pubVal + '\'' +
				", data=" + Arrays.deepToString(data) +
				", otherObject=" + otherObject +
				", list=[" + list.stream().map(Arrays::toString).collect(Collectors.joining(", "))+"]" +
				", someEnum=" + someEnum +
				'}';
	}

	public static void main(String[] args) throws IOException {
		try(BufferedReader br=new BufferedReader(new InputStreamReader(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("testClass.json")), StandardCharsets.UTF_8))){
			String json= String.join("", br.lines().collect(Collectors.joining()));
			TestClass obj = TestClassJSONLoader.fromJSON(json);
			System.out.println(obj);
		}
		TestClass testObj=new TestClass();
		testObj.setProp(12345);
		testObj.pubVal="test";

		ReferencedClass listElem=new ReferencedClass();
		listElem.setI(100L);
		testObj.list.add(new ReferencedClass[]{new ReferencedClass(),listElem,null});
		testObj.list.add(null);

		testObj.list.add(new ReferencedClass[0]);

		testObj.someEnum=TestEnum.C;

		System.out.println(TestClassJSONLoader.toJSON(testObj));
	}
	public enum TestEnum{
		A,B,C
	}
}
