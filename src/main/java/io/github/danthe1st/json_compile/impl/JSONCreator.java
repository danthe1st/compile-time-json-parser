package io.github.danthe1st.json_compile.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import io.github.danthe1st.json_compile.impl.data.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.tools.JavaFileObject;

import io.github.danthe1st.json_compile.api.GenerateJSON;

@SupportedAnnotationTypes("io.github.danthe1st.json_compile.api.GenerateJSON")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class JSONCreator extends AbstractProcessor {

    private static final String JSONOBJECT_PARAM_NAME = "data";

    private static final Map <String, String> simpleAssignments = Map.ofEntries(Map.entry("java.lang.String", "String"),
            Map.entry("int", "Int"),Map.entry("java.lang.Integer","Int"),
            Map.entry("long", "Long"),Map.entry("java.lang.Long","Long"),
            Map.entry("boolean","Boolean"),Map.entry("java,lang.Boolean","Boolean"),
            Map.entry("float","Float"),Map.entry("java,lang.Float","Float"),
            Map.entry("double","Double"),Map.entry("java,lang.Double","Double"));

    @Override
    public boolean process(Set <? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(Element element : roundEnv.getElementsAnnotatedWith(GenerateJSON.class)) {
            try {
                generateJSON(element);
            } catch(IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR,
                        "An I/O error occured during processing element: " + e.getMessage(), element);
            }
        }
        return false;
    }

    private void generateJSON(Element element) throws IOException {
        if(element.getKind() == ElementKind.CLASS) {
            DeclaredType typeMirror = (DeclaredType) element.asType();
            String newClassName = typeMirror.toString() + "JSONLoader";
            if(element.getEnclosingElement()!=null&&element.getEnclosingElement().getKind()!=ElementKind.PACKAGE){
                processingEnv.getMessager().printMessage(Kind.ERROR,"Nested elements are not supported",element);
                return;
            }

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

                    operations.add(new FieldOperation(innerElement.getSimpleName().toString(), innerElement.asType()));
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

    private void addPropertyFromJSON(ClassWriter writer, JSONOperation jsonOperation,String jsonObjectName) throws IOException {
        TypeMirror type = jsonOperation.getType();
        String typeName = type.toString();
        String val = null;
        if(hasVisibilityProblems(type,true)){
            return;
        }
        if(type.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) type;

            String jsonArrayName = addCreateJSONArrayCode(writer, jsonOperation, jsonObjectName);

            String actualArrayName = jsonOperation.getAttributeName().replaceAll("\\[.*]", "") + "DataArray";
            writer.addAssignment(arrayType.getComponentType().toString() + "[] " + actualArrayName, "null");

            writer.beginIf(jsonArrayName + "!=null");

            String componentName = arrayType.getComponentType().toString();
            int componentNameArrayStart = componentName.indexOf('[');
            String componentNameBefore = componentName;
            String componentNameAfter = "";
            if(componentNameArrayStart != -1) {
                componentNameBefore = componentName.substring(0, componentNameArrayStart);
                componentNameAfter = componentName.substring(componentNameArrayStart);
            }

            writer.addAssignment(actualArrayName, "new " + componentNameBefore + "[" + jsonArrayName + ".length()]" + componentNameAfter + " ");

            String counterVar = jsonArrayName + "Counter";
            writer.beginSimpleFor("int " + counterVar + "=0", counterVar + "<" + jsonArrayName + ".length()", counterVar + "++");

            addPropertyFromJSON(writer, new ArrayElementOperation(actualArrayName + "[" + counterVar + "]", arrayType.getComponentType()), jsonArrayName);

            writer.endFor();
            val = actualArrayName;
            writer.endIf();
        }else if(isCollection(type)){
            TypeMirror collectionType=getCollectionType(type);
            if(collectionType==null){
                processingEnv.getMessager().printMessage(Kind.ERROR,"Cannot infer type",type instanceof DeclaredType? ((DeclaredType) type).asElement() : null);
                return;
            }
            String jsonArrayName=addCreateJSONArrayCode(writer,jsonOperation,jsonObjectName);
            String counterVar=jsonArrayName+"Counter";

            String dataName=jsonArrayName+"Data";
            writer.addAssignment(typeName+" "+dataName,jsonOperation.getAccessor("ret"));
            writer.beginIf(jsonArrayName+"!=null");
            writer.beginSimpleFor("int "+counterVar+"=0",counterVar+"<"+jsonArrayName+".length()",counterVar+"++");


            addPropertyFromJSON(writer,new CollectionElementOperation(dataName,counterVar,collectionType),jsonArrayName);

            val=dataName;

            writer.endIf();
            writer.endFor();
        } else if(type instanceof DeclaredType&&((DeclaredType) type).asElement().getKind()==ElementKind.ENUM){
            val = jsonObjectName + ".optEnum("+type+".class," + jsonOperation.getJSONAccessName() + ")";
        }else if(simpleAssignments.containsKey(typeName)) {
            String name=jsonOperation.getJSONAccessName();
            val = jsonObjectName + ".opt" + simpleAssignments.get(typeName) + "(" + name + ")";
        } else {
            TypeElement referencedElement = processingEnv.getElementUtils().getTypeElement(typeName);
            if(referencedElement != null && referencedElement.getAnnotation(GenerateJSON.class) != null) {
                val = referencedElement + "JSONLoader.fromJSON(" + jsonObjectName + ".optJSONObject(" + jsonOperation.getJSONAccessName() + "))";
            }
        }
        if(val == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "type " + typeName + " is not supported");
        } else {
            writer.addStatement(jsonOperation.getMutator("ret",val));
        }
    }

    private String addCreateJSONArrayCode(ClassWriter writer, JSONOperation jsonOperation, String jsonObjectName) throws IOException {
        String jsonArrayName=jsonObjectName+capitalizeFirst(jsonOperation.getAttributeName()).replaceAll("\\[.*]","")+"JsonArray";
        String optArgument=jsonOperation.getJSONAccessName();
        writer.addAssignment(JSONArray.class.getCanonicalName() +" "+jsonArrayName,jsonObjectName+".optJSONArray("+optArgument+")");
        return jsonArrayName;
    }

    private boolean isCollection(TypeMirror type) {
        return getCollectionType(type)!=null;
    }

    private TypeMirror getCollectionType(TypeMirror type){
        if(!(type instanceof DeclaredType)) {
            return null;
        }
        TypeElement elem= (TypeElement) ((DeclaredType) type).asElement();
        for(TypeMirror iFace : elem.getInterfaces()) {
            if(Collection.class.getCanonicalName().equals(((DeclaredType)iFace).asElement().toString())){

                List <? extends TypeMirror> typeArgs = ((DeclaredType) iFace).getTypeArguments();
                if(typeArgs.size()==1){
                    TypeMirror genericArg = typeArgs.get(0);
                    if(genericArg.getKind()==TypeKind.TYPEVAR){
                        for(int i = 0; i < elem.getTypeParameters().size(); i++) {
                            if(elem.getTypeParameters().get(i).getSimpleName().toString().equals(genericArg.toString())){
                                return ((DeclaredType) type).getTypeArguments().get(i);
                            }
                        }
                    }
                    return genericArg;
                }
                return null;
            }
        }
        return null;
    }

    private boolean hasVisibilityProblems(TypeMirror type,boolean displayError){
        if(type instanceof DeclaredType){
            DeclaredType declType= (DeclaredType) type;
            if(!declType.asElement().getModifiers().contains(Modifier.PUBLIC)){
                if(displayError){
                    processingEnv.getMessager().printMessage(Kind.ERROR,"only public types are supported", declType.asElement());
                }
                return true;
            }
        }
        return false;
    }

    private void addPropertyToJSON(ClassWriter writer, JSONOperation jsonOperation,String jsonObjectName) throws IOException {
        TypeMirror type = jsonOperation.getType();
        String typeName = type.toString();
        String val = jsonOperation.getAccessor("obj");
        if(hasVisibilityProblems(type,false)){
            return;
        }
        if(val == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "type " + typeName + " is currenly not supported");
        }else if(type.getKind()==TypeKind.ARRAY){
            ArrayType arrayType = (ArrayType) type;

            String jsonArrayName=jsonObjectName+jsonOperation.getAttributeName().replaceAll("\\[.*]","");
            String arrayName=jsonArrayName+"Data";
            writer.addAssignment(arrayType.getComponentType().toString()+"[] "+arrayName,val);
            writer.beginIf(arrayName+"!=null");
            writer.addAssignment(JSONArray.class.getCanonicalName()+" "+jsonArrayName,"new "+JSONArray.class.getCanonicalName()+"()");

            String counterVar=jsonArrayName+"Counter";
            writer.beginSimpleFor("int "+counterVar+"=0",counterVar+"<"+arrayName+".length",counterVar+"++");

            addPropertyToJSON(writer, new ArrayElementOperation(arrayName+"["+counterVar+"]",arrayType.getComponentType()), jsonArrayName);

            writer.endFor();
            if(jsonOperation.isChildType()){
                writer.addMethodCall(jsonObjectName,"put",jsonArrayName);
            }else{
                writer.addMethodCall(jsonObjectName,"put","\""+jsonOperation.getAttributeName()+"\"",jsonArrayName);
            }
            writer.endIf();
        }else if(isCollection(type)){
            if(jsonOperation.isChildType()){
                processingEnv.getMessager().printMessage(Kind.ERROR,"Collections of collections are not supported",type instanceof DeclaredType? ((DeclaredType) type).asElement() : null);
                return;
            }
            TypeMirror collectionType = getCollectionType(type);
            if(collectionType==null){
                processingEnv.getMessager().printMessage(Kind.ERROR,"Cannot infer type",type instanceof DeclaredType? ((DeclaredType) type).asElement() : null);
                return;
            }

            String dataName=jsonObjectName+capitalizeFirst(jsonOperation.getAttributeName()).replaceAll("\\[.*]","");
            String collectionName=dataName+"Collection";
            String jsonArrayName=dataName+"JSONArray";

            writer.addAssignment(JSONArray.class.getCanonicalName()+" "+jsonArrayName,"new "+JSONArray.class.getCanonicalName()+"()");
            writer.addAssignment(type +" "+collectionName,val);
            writer.beginIf(collectionName+"!=null");
            writer.beginForEach(collectionType.toString(),dataName,collectionName);

            addPropertyToJSON(writer,new CollectionElementOperation(collectionName,dataName,collectionType),jsonArrayName);

            writer.endFor();
            writer.endIf();

            if(jsonOperation.isChildType()){
                writer.addMethodCall(jsonObjectName,"put",jsonArrayName);
            }else{
                writer.addMethodCall(jsonObjectName,"put","\""+jsonOperation.getAttributeName()+"\"",jsonArrayName);
            }
        } else if(type instanceof DeclaredType&&((DeclaredType) type).asElement().getKind()==ElementKind.ENUM){
            if(jsonOperation.isChildType()){
                writer.addMethodCall(jsonObjectName,"put",val);
            }else{
                writer.addMethodCall(jsonObjectName,"put","\""+jsonOperation.getAttributeName()+"\"",val);
            }
        } else if(simpleAssignments.containsKey(typeName)) {
            if(jsonOperation.isChildType()){
                writer.addMethodCall(jsonObjectName, "put", val);
            }else {
                writer.addMethodCall(jsonObjectName, "put", "\"" + jsonOperation.getAttributeName() + "\"", val);
            }
        } else {
            TypeElement referencedElement = processingEnv.getElementUtils().getTypeElement(typeName);
            if(referencedElement != null && referencedElement.getAnnotation(GenerateJSON.class) != null) {
                if(jsonOperation.isChildType()){
                    writer.addMethodCall(jsonObjectName, "put", referencedElement + "JSONLoader.toJSONObject(" + val + ")");
                }else{
                    writer.addMethodCall(jsonObjectName, "put", "\"" + jsonOperation.getAttributeName() + "\"", referencedElement + "JSONLoader.toJSONObject(" + val + ")");
                }
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
                    return new PropertyOperation(propName,
                            ((ExecutableElement) element).getReturnType());
                }
            }
        }
        return null;
    }

    public static String capitalizeFirst(String toCapitalize){
        return Character.toUpperCase(toCapitalize.charAt(0)) + (toCapitalize.length() > 1 ? toCapitalize.substring(1):"");
    }
}
