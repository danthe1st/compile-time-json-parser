# Compile-time JSON-parser [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.danthe1st/compile-time-json-parser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.danthe1st/compile-time-json-parser)
> generates a JSON-parser for Java-objects at compile-time

Compile-time JSON-parser supports both non-private variables and properties.

The generated JSON-parser uses `org.json:json`.

### Setup
* [Download](https://maven.apache.org/download.cgi) and [install](https://maven.apache.org/install.html) [Maven](https://maven.apache.org/)
* Download the sources
* Run `mvn clean install` in the directory of Compile-time JSON-parser
* Create a Maven Project in IntelliJ where you want to use Compile-time JSON-parser
* Add the following dependency to the `pom.xml` of the project where you want to use Compile-time JSON-parser (replace `VERSION` with the version from [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.danthe1st/compile-time-json-parser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.danthe1st/compile-time-json-parser))
```xml
<dependency>
    <groupId>io.github.danthe1st</groupId>
    <artifactId>compile-time-json-parser</artifactId>
    <version>VERSION</version>
</dependency>
```
* If you wish to use JPMS, also add the annotation processor to the `maven-compiler-plugin`
```xml
<plugin>
	<artifactId>maven-compiler-plugin</artifactId>
	<version>3.8.1</version>
	<configuration>
		<release>11</release>
		<annotationProcessorPaths>
			<annotationProcessorPath>
				<groupId>io.github.danthe1st</groupId>
				<artifactId>compile-time-json-parser</artifactId>
				<version>0.0.1-SNAPSHOT</version>
			</annotationProcessorPath>
		</annotationProcessorPaths>
	</configuration>
</plugin>
```
* Enable annotation processing for this project under `Settings`>`Build, Execution, Deployment`>`Compiler`>`Annotation Processors`>`Enable Annotation Processing`

### Usage
* Create a data class and annotate it with `@GenerateJSON` like this:

```java
import io.github.danthe1st.json_compile.api.GenerateJSON;
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
}
```
* When compiling the class, a class suffixed with `JSONLoader` should be automatically generated.<br/>
  This class contains a method named `fromJSON` that creates an instance of the data class from a `String`:
  
```java
String json= String.join("", Files.readAllLines(Path.of("testClass.json")));
TestClass obj = TestClassJSONLoader.fromJSON(json);
System.out.println(obj);
TestClass testObj=new TestClass();
testObj.setSomeString("test");
testObj.someInt=12345;
testObj.someArray=new int[][]{{1,2,3},{},null,{1,2,3,4,5,6}};
System.out.println(TestClassJSONLoader.toJSON(testObj));
```

### Example

An example project can be found in the directory `examples/maven-example`.

* Import `Compile-time JSON-parser` in IntelliJ as a maven project
* Run `mvn clean install` in that project
* Expand the `examples/maven-example` directory
* Right-click on the file `pom.xml` in that directory and select `Add as a Maven Project`
* [Make sure you set up your IDE correctly](#ide-specific-configuration)
* Run `TestClass` in `examples/maven-example/src/main/java/io/github/danthe1st/json_compile/test/TestClass`

### Supported types
* `String`
* `int`
* `long`
* `float`
* `double`
* `boolean`
* Enums
* Wrapper classes for supported primitive types
* Objects of classes annotated with `@GenerateJSON`
* `Collection`s if they are not part of other collections or arrays, `Collections` of classes annotated with `@GenerateJSON` that contain collections are supported, however
* Arrays

### Limitations
* It is not possible to create an array/collection of collections
* Objects annotated with `@GenerateJSON` need to have a no-args-constructor
* Collections need to be initialized in the constructor
* Generic objects are not supported (except generic collections)
* Configuration is not supported

### IDE-specific configuration

### Eclipse
* Install the [m2e-apt plugin](https://marketplace.eclipse.org/content/m2e-apt)
  [![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.svg)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1216155 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client")

![m2e-apt](https://user-images.githubusercontent.com/34687786/114560029-9713f280-9c6c-11eb-889d-096ee46b52c0.png)

* Right-click the project, open `Properties`>`Java Compiler`>`Annotation Processing`> and select `Enable Project Specific Settings` and `Enable processing in Editor`

[Video](https://user-images.githubusercontent.com/34687786/114566218-56b77300-9c72-11eb-88f0-6c030fe4e8ac.mp4)

### IntelliJ
* Enable annotation processing for this project under `Settings`>`Build, Execution, Deployment`>`Compiler`>`Annotation Processors`>`Maven default annotation processors profile`>`json-parser-maven-example`>`Enable Annotation Processing`

[Video](https://user-images.githubusercontent.com/34687786/114572301-a6e50400-9c77-11eb-9a2d-de3bdac3688a.mp4)
