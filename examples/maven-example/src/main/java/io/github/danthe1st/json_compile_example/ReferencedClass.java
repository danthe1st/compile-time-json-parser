package io.github.danthe1st.json_compile_example;

import io.github.danthe1st.json_compile.api.GenerateJSON;

@GenerateJSON
public class ReferencedClass {
    private Long i;

    public Long getI() {
        return i;
    }

    public void setI(Long i) {
        this.i = i;
    }

    @Override
    public String toString() {
        return "ReferencedClass{" +
                "i=" + i +
                '}';
    }
}
