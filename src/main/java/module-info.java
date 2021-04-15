module io.github.danthe1st.json_compile{
	requires java.base;
	requires java.compiler;
	requires org.json;
	exports io.github.danthe1st.json_compile.api;
	provides javax.annotation.processing.Processor with io.github.danthe1st.json_compile.impl.JSONCreator;
}
