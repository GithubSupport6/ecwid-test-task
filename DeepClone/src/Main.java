
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

class SampleSubclass {

    public SampleSubclass(){

    }

    String value1 = "value1";
    String value2 = "value2";
}


class SampleClass {

    public SampleClass(){
        simpleList.add("oneList");
        simpleList.add("twoList");
    }

    String simpleValue = "simpleValue";
    SampleSubclass simpleInnerClass = new SampleSubclass();
    SampleClass selfReference = this;
    LinkedList<String> simpleList = new LinkedList<>();
    String[] simpleArray = {"one", "two"};
    LinkedList<SampleSubclass> subclassList = new LinkedList<>();

    private String getData(Object[] array){
        StringBuilder res = new StringBuilder("[");
        for (Object el:array) {
            res.append(el.toString()).append(",");
        }
        res.deleteCharAt(res.length()-1);
        res.append("]");
        return res.toString();
    }

    private String getData(Collection<?> collection){
        StringBuilder res = new StringBuilder("[");
        for (Object el:collection) {
            res.append(el.toString()).append(",");
        }
        res.deleteCharAt(res.length()-1);
        res.append("]");
        return res.toString();
    }

    @Override
    public String toString() {
        String array = getData(simpleArray);
        String list = getData(simpleList);
        return  "{ " + simpleValue + ":"
                + simpleInnerClass.value1 + ":"
                + simpleInnerClass.value2 + ":"
                + selfReference.simpleValue + ":"
                + array + ":"
                + list + ":"
                + subclassList.isEmpty() + " }";
    }
}

class CopyUtils {

    private enum FieldType {
        SIMPLE_TYPE,
        ARRAY,
        COLLECTION,
        CLASS
    }

    final static String STRING_TYPE = "class java.lang.String";
    final static String COLLECTION_TYPE = "java.lang.Collection";

    static HashSet<Object> linkCounter = new HashSet<>();

    private static void getInterface(List<String> interfaces, Class<?> clazz){
        Class<?>[] classInterfaces = clazz.getInterfaces();
        if (classInterfaces.length == 0){
            interfaces.add(clazz.getName());
            return;
        }
        for (Class<?> iface : classInterfaces) {
            getInterface(interfaces,iface);
        }
    }

    private static FieldType defineType(Field field, Object obj) throws IllegalAccessException {
        if (field.getType().isPrimitive() || field.get(obj) instanceof String){
            return FieldType.SIMPLE_TYPE;
        }
        else if (field.getType().isArray()){
            System.out.println(field.getType().getSuperclass());
            return FieldType.ARRAY;
        }
        else if (field.get(obj) instanceof Collection<?>){
            return FieldType.COLLECTION;
        }
        else return FieldType.CLASS;
    }

    private static Object copyObjectOrCopyReference(Object fieldObject) throws Exception {
        if (!linkCounter.contains(fieldObject)) {
            return deepCopy(fieldObject);
        }
        return fieldObject;
    }

    private static <T> void setFieldInner(T currentObject, T newObject, Field field) throws Exception {
        FieldType fieldType = defineType(field,currentObject);
        if (fieldType == FieldType.SIMPLE_TYPE){
            field.setAccessible(true);
            field.set(newObject,field.get(currentObject));
        }
        else if (fieldType == FieldType.ARRAY){
            Object[] array = (Object[]) field.get(currentObject);

            int length = Array.getLength(array);
            Object[] newArray = (Object[])Array.newInstance(field.getType().componentType(),length);
            System.arraycopy(array, 0, newArray, 0, newArray.length);
            field.set(newObject,newArray);
        }
        else if (fieldType == FieldType.COLLECTION) {
            Collection<?> collection = (Collection<?>) field.get(currentObject);
            Collection<Object> newCollection = (Collection<Object>) field.get(currentObject).getClass().getConstructor().newInstance();
            for (Object el:collection){
                Object obj = copyObjectOrCopyReference(el);
                newCollection.add(obj);
            }
            field.set(newObject,newCollection);
        }
        else if (fieldType == FieldType.CLASS){
            Object obj = copyObjectOrCopyReference(field.get(currentObject));
            field.set(newObject,obj);
        }

    }

    public static <T> T deepCopy(T obj) throws Exception {

        linkCounter.add(obj);

        if (obj.getClass().isPrimitive()){
            return obj;
        }
        if (obj instanceof String){
            String newString = new String(obj.toString());
            return (T)newString;
        }
        //doesn't work for inner classes
        T newObject = (T) obj.getClass().getConstructor().newInstance();
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field: fields) {
            setFieldInner(obj,newObject,field);
        }
        return newObject;
    }
}

public class Main {

    public static void main(String[] args) {
        SampleClass obj1 = new SampleClass();
        obj1.simpleValue = "hello";
        SampleClass obj2 = null;
        obj1.simpleInnerClass.value1 = "simpleInnerClassValue1";
        try {
            obj2 = CopyUtils.deepCopy(obj1);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        System.out.println("Obj1: " + obj1);
        System.out.println("Obj2: " + obj2);

        obj2.simpleValue = "valueChanged";
        obj2.simpleArray[1] = "three";
        obj2.simpleList.set(0,"hello");
        obj2.simpleInnerClass.value1 = "classValue";

        System.out.println("Obj1: " + obj1);
        System.out.println("Obj2: " + obj2);
    }
}