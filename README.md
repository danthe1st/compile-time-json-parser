# Compile-time JSON-parser
> generates a JSON-parser for Java-objects at compile-time

Compile-time JSON-parser supports both non-private variables and properties.

The generated JSON-parser uses `org.json:json`.

### Setup
* [Download](https://maven.apache.org/download.cgi) and [install](https://maven.apache.org/install.html) [Maven](https://maven.apache.org/)
* Download the sources
* Run `mvn clean install` in the directory of Compile-time JSON-parser
* Create a Maven Project in IntelliJ where you want to use Compile-time JSON-parser
* Add the following dependency to the `pom.xml` of the project where you want to use Compile-time JSON-parser
```xml
<dependency>
    <groupId>io.github.danthe1st</groupId>
    <artifactId>compile-time-json-parser</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
* Enable annotation processing for this project under `Settings`>`Build, Execution, Deployment`>`Compiler`>`Annotation Processors`>`Enable Annotation Processing`

### Usage
* Create a data class and annotate it with `@GenerateJSON` like this:
```java
import io.github.danthe1st.json_compile.api.GenerateJSON;
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
}
```
* When compiling the class, a class suffixed with `JSONLoader` should be automatically generated.<br/>
  This class contains a method named `fromJSON` that creates an instance of the data class from a `String`:
```java
String json= String.join("", Files.readAllLines(Path.of("testClass.json")));
TestClass obj = TestClassJSONLoader.fromJSON(json);
System.out.println(obj);
```

### Example

An example project can be found in the directory `examples/maven-example`.

* Import `Compile-time JSON-parser` in IntelliJ as a maven project
* Expand the `examples/maven-example` directory
* Right-click on the file `pom.xml` in that directory and select `Add as a Maven Project`
* Enable annotation processing for this project under `Settings`>`Build, Execution, Deployment`>`Compiler`>`Annotation Processors`>`Enable Annotation Processing`
* Run `TestClass` in `examples/maven-example/src/main/java/io/github/danthe1st/json_compile/test/TestClass`

### Limitations

* Currently, the only supported data types are `int` and `String`.
* Eclipse may not detect the annotation processor
* Converting objects to JSON is currently not supported
