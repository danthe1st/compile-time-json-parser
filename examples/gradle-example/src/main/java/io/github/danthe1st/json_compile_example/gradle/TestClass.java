package io.github.danthe1st.json_compile_example.gradle;

import io.github.danthe1st.json_compile.api.GenerateJSON;
import io.github.danthe1st.json_compile_example.gradle.TestClassJSONLoader;

@GenerateJSON
public class TestClass {
    private int i;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    @Override
    public String toString() {
        return "TestClass{" +
                "i=" + i +
                '}';
    }

    public static void main(String[] args) {
        System.out.println(TestClassJSONLoader.fromJSON("{'i':1337}"));
        TestClass obj=new TestClass();
        obj.setI(123);
        System.out.println(TestClassJSONLoader.toJSON(obj));
    }
}
