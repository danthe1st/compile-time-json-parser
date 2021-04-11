package io.github.danthe1st.json_compile.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.tools.JavaFileObject;

import io.github.danthe1st.json_compile.api.GenerateJSON;
import io.github.danthe1st.json_compile.impl.data.JSONOperation;
import io.github.danthe1st.json_compile.impl.data.JSONOperation.OperationType;
import io.github.danthe1st.json_compile.impl.data.VariableDefinition;

@SupportedAnnotationTypes("io.github.danthe1st.json_compile.api.GenerateJSON")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class JSONCreator extends AbstractProcessor {

    private static final String JSONOBJECT_PARAM_NAME = "data";

    private static final Map <String, String> simpleAssignments = Map.of("java.lang.String", "String", "int", "Int", "long", "Long");

    @Override
    public boolean process(Set <? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(Element element : roundEnv.getElementsAnnotatedWith(GenerateJSON.class)) {
            try {
                generateJSON(element);
            } catch(IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "An I/O error occured during processing element: " + e.getMessage(), element);
            }
            processingEnv.getMessager().printMessage(Kind.NOTE, "Element detected", element);
        }
        return false;
    }

    private void generateJSON(Element element) throws IOException {
        if(element.getKind() == ElementKind.CLASS) {
            DeclaredType typeMirror = (DeclaredType) element.asType();
            String newClassName = typeMirror.toString() + "JSONLoader";

            JavaFileObject loaderSource = processingEnv.getFiler().createSourceFile(newClassName, element);
            try(ClassWriter writer = new ClassWriter(new BufferedWriter(loaderSource.openWriter()))) {
                generateJSON(element, newClassName, writer);
            }
        } else {
            processingEnv.getMessager().printMessage(Kind.ERROR,
                    "Only instanciable classes should be annotated with " + GenerateJSON.class.getCanonicalName(),
                    element);
        }
    }

    private void generateJSON(Element element, String fullyQualidiedClassName, ClassWriter writer) throws IOException {
        writer.packageDeclaration(processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString());
        writer.addImport("org.json.JSONObject");
        writer.beginClass(getSimpleClassName(fullyQualidiedClassName));
        boolean noArgsConstructorFound = false;

        List <JSONOperation> operations = new ArrayList <>();

        for(Element innerElement : element.getEnclosedElements()) {
            if(innerElement.getModifiers().contains(Modifier.TRANSIENT) || innerElement.getModifiers().contains(Modifier.PRIVATE)
                    || innerElement.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            switch(innerElement.getKind()) {
                case CONSTRUCTOR:
                    noArgsConstructorFound = noArgsConstructorFound
                            || ((ExecutableElement) innerElement).getParameters().isEmpty();
                    break;
                case FIELD:

                    operations.add(new JSONOperation(((VariableElement) innerElement).getSimpleName().toString(),
                            OperationType.FIELD, innerElement.asType()));
                    break;
                case METHOD:
                    JSONOperation op = loadMethodInfo(innerElement);
                    if(op != null) {
                        operations.add(op);
                    }
                    break;

                default:
                    break;
            }
        }
        writer.beginMethod("fromJSON", element.toString(),
                new VariableDefinition(JSONObject.class.getCanonicalName(), JSONOBJECT_PARAM_NAME));
        addReturnIfNull(writer, JSONOBJECT_PARAM_NAME);
        String nameOfClassToCreate = ((TypeElement) element).getQualifiedName().toString();
        writer.addVariable(new VariableDefinition(nameOfClassToCreate, "ret"), "new " + nameOfClassToCreate + "()");

        for(JSONOperation jsonOperation : operations) {
            addPropertyFromJSON(writer, jsonOperation,JSONOBJECT_PARAM_NAME);
        }

        writer.addReturn("ret");
        writer.endMethod();
        if(!noArgsConstructorFound) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Missing public no-args-constructor", element);
        }

        writer.beginMethod("fromJSON", element.toString(),
                new VariableDefinition(String.class.getCanonicalName(), JSONOBJECT_PARAM_NAME));
        addReturnIfNull(writer, JSONOBJECT_PARAM_NAME);
        writer.addReturn("fromJSON(new " + JSONObject.class.getCanonicalName() + "(" + JSONOBJECT_PARAM_NAME + "))");
        writer.endMethod();

        writer.beginMethod("toJSONObject", JSONObject.class.getCanonicalName(),
                new VariableDefinition(element.toString(), "obj"));
        addReturnIfNull(writer, "obj");
        writer.addAssignment(JSONObject.class.getCanonicalName() + " " + JSONOBJECT_PARAM_NAME, "new " + JSONObject.class.getCanonicalName() + "()");

        for(JSONOperation jsonOperation : operations) {
            addPropertyToJSON(writer, jsonOperation,JSONOBJECT_PARAM_NAME);
        }
        writer.addReturn("data");
        writer.endMethod();

        writer.beginMethod("toJSON", String.class.getCanonicalName(),
                new VariableDefinition(element.toString(), "obj"));
        addReturnIfNull(writer, "obj");
        writer.addReturn("toJSONObject(obj).toString()");
        writer.endMethod();

        writer.endClass();
    }

    //TODO arrays/collections, more primitives
    private void addPropertyFromJSON(ClassWriter writer, JSONOperation jsonOperation,String jsonObjectName) throws IOException {
        TypeMirror type = jsonOperation.getType();
        String typeName = type.toString();
        String val = null;
        if(type.getKind() == TypeKind.ARRAY) {
            //TODO test, add in addPropertyToJSON
            ArrayType arrayType = (ArrayType) type;

            String jsonArrayName=jsonObjectName+capitalizeFirst(jsonOperation.getAttributeName()).replaceAll("\\[.*]","")+"JsonArray";
            String optArgument=getJSONAccessName(jsonOperation);
            writer.addAssignment(JSONArray.class.getCanonicalName() +" "+jsonArrayName,jsonObjectName+".optJSONArray("+optArgument+")");

            String actualArrayName=jsonOperation.getAttributeName().replaceAll("\\[.*]","")+"DataArray";
            writer.addAssignment(arrayType.getComponentType().toString()+"[] "+actualArrayName,"null");

            writer.beginIf(jsonArrayName+"!=null");

            String componentName=arrayType.getComponentType().toString();
            int componentNameArrayStart=componentName.indexOf('[');
            String componentNameBefore=componentName;
            String componentNameAfter="";
            if(componentNameArrayStart!=-1){
                componentNameBefore=componentName.substring(0,componentNameArrayStart);
                componentNameAfter=componentName.substring(componentNameArrayStart);
            }

            writer.addAssignment(actualArrayName,"new "+componentNameBefore+"["+jsonArrayName+".length()]"+componentNameAfter+" ");

            String counterVar=jsonArrayName+"Counter";
            writer.beginSimpleFor("int "+counterVar+"=0",counterVar+"<"+jsonArrayName+".length()",counterVar+"++");

            addPropertyFromJSON(writer,new JSONOperation(actualArrayName+"["+counterVar+"]",OperationType.ARRAY_ELEMENT,arrayType.getComponentType()),jsonArrayName);

            writer.endFor();
            val=actualArrayName;
            writer.endIf();
        } else if(simpleAssignments.containsKey(typeName)) {
            String name=getJSONAccessName(jsonOperation);
            val = jsonObjectName + ".opt" + simpleAssignments.get(typeName) + "(" + name + ")";
        } else {
            TypeElement referencedElement = processingEnv.getElementUtils().getTypeElement(typeName);
            if(referencedElement != null && referencedElement.getAnnotation(GenerateJSON.class) != null) {
                val = referencedElement.toString() + "JSONLoader.fromJSON(" + jsonObjectName + ".optJSONObject(\"" + jsonOperation.getAttributeName() + "\"))";
            }
        }
        if(val == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "type " + typeName + " is not supported");
        } else {
            switch(jsonOperation.getOpType()) {
                case FIELD:
                    writer.addAssignment("ret." + jsonOperation.getAttributeName(), val);
                    break;
                case PROPERTY:
                    writer.addMethodCall("ret", "set" + capitalizeFirst(jsonOperation.getAttributeName()), val);
                    break;
                case ARRAY_ELEMENT:
                    writer.addAssignment(jsonOperation.getAttributeName(),val);
                    break;
            }
        }
    }

    private String getJSONAccessName(JSONOperation jsonOperation){
        String name=jsonOperation.getAttributeName();
        if(jsonOperation.getOpType()==OperationType.ARRAY_ELEMENT){
            name=name.substring(name.indexOf('[')+1,name.indexOf(']'));
        }else{
            name='"'+name+'"';
        }
        return name;
    }

    private void addPropertyToJSON(ClassWriter writer, JSONOperation jsonOperation,String jsonObjectName) throws IOException {
        TypeMirror type = jsonOperation.getType();
        String typeName = type.toString();
        String val = null;
        switch(jsonOperation.getOpType()) {
            case FIELD:
                val = "obj." + jsonOperation.getAttributeName();
                break;
            case PROPERTY:
                val = "obj.get" + capitalizeFirst(jsonOperation.getAttributeName()) + "()";
                break;
            case ARRAY_ELEMENT:
                val=jsonOperation.getAttributeName();
        }
        if(val == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "type " + typeName + " is currenly not supported");
        }else if(type.getKind()==TypeKind.ARRAY){
            //TODO
            ArrayType arrayType = (ArrayType) type;

            String jsonArrayName=jsonObjectName+jsonOperation.getAttributeName().replaceAll("\\[.*]","");
            String arrayName=jsonArrayName+"Data";
            writer.addAssignment(arrayType.getComponentType().toString()+"[] "+arrayName,val);
            writer.beginIf(arrayName+"!=null");
            writer.addAssignment(JSONArray.class.getCanonicalName()+" "+jsonArrayName,"new "+JSONArray.class.getCanonicalName()+"()");

            String counterVar=jsonArrayName+"Counter";
            writer.beginSimpleFor("int "+counterVar+"=0",counterVar+"<"+arrayName+".length",counterVar+"++");

            writer.addMethodCall(jsonArrayName,"put",arrayName+"["+counterVar+"]");//TODO recursion?

            writer.endFor();
            writer.addMethodCall(jsonObjectName,"put","\""+jsonOperation.getAttributeName()+"\"",jsonArrayName);
            writer.endIf();
        } else if(simpleAssignments.containsKey(typeName)) {
            writer.addMethodCall(jsonObjectName, "put", "\"" + jsonOperation.getAttributeName() + "\"", val);
        } else {
            TypeElement referencedElement = processingEnv.getElementUtils().getTypeElement(typeName);
            if(referencedElement != null && referencedElement.getAnnotation(GenerateJSON.class) != null) {
                writer.addMethodCall(jsonObjectName, "put", "\"" + jsonOperation.getAttributeName() + "\"", referencedElement.toString() + "JSONLoader.toJSONObject(" + val + ")");
            }
        }

    }

    private void addReturnIfNull(ClassWriter writer, String paramName) throws IOException {
        writer.beginIf(paramName + "==null");
        writer.addReturn("null");
        writer.endIf();
    }

    private static String getSimpleClassName(String fullyQualifiedClassName) {
        String[] split = fullyQualifiedClassName.split("\\.");
        return split[split.length - 1];
    }

    private JSONOperation loadMethodInfo(Element element) {
        String name = element.getSimpleName().toString();
        int nameLen = name.length();
        if(name.startsWith("get") && nameLen > 3 && element.getKind() == ElementKind.METHOD
                && processingEnv.getTypeUtils().getNoType(TypeKind.VOID) != element.asType()) {
            String propName = Character.toLowerCase(name.charAt(3)) + (nameLen > 4 ? name.substring(4) : "");
            for(Element sibling : element.getEnclosingElement().getEnclosedElements()) {
                if(sibling.getKind() == ElementKind.METHOD
                        && ("set" + name.substring(3)).equals(sibling.getSimpleName().toString())) {
                    return new JSONOperation(propName, OperationType.PROPERTY,
                            ((ExecutableElement) element).getReturnType());
                }
            }
        }
        return null;
    }

    private String capitalizeFirst(String toCapitalize){
        return Character.toUpperCase(toCapitalize.charAt(0)) + (toCapitalize.length() > 1 ? toCapitalize.substring(1):"");
    }
}
