
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

class SampleSubclass {

    public SampleSubclass(){

    }

    String value1 = "value1";
    String value2 = "value2";

    @Override
    public String toString() {
        return "{" + value1 + "," + value2 + "}";
    }
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
        String sublist = getData(subclassList);
        return  "{\n       " + simpleValue + ",\n"
                + "       " + simpleInnerClass.value1 + ",\n"
                + "       " + simpleInnerClass.value2 + ",\n"
                + "       " + selfReference.simpleValue + ",\n"
                + "       " + array + ",\n"
                + "       " + list + ",\n"
                + "       " + sublist + "\n }";
    }
}

class CopyUtils {

    private enum FieldType {
        SIMPLE_TYPE,
        ARRAY,
        COLLECTION,
        CLASS
    }

    static HashSet<Object> linkCounter = new HashSet<>();

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
            for (int i = 0;i<array.length;i++){
                Object obj = copyObjectOrCopyReference(array[i]);
                newArray[i] = obj;
            }
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
        obj1.simpleValue = "object1";
        obj1.simpleInnerClass.value1 = "innerValue1";
        obj1.simpleInnerClass.value2 = "innerValue2";
        obj1.simpleList = new LinkedList<String>();
        obj1.simpleList.add("list1");
        obj1.simpleList.add("list1");
        obj1.simpleArray = new String[2];
        obj1.simpleArray[0] = "array1";
        obj1.simpleArray[1] = "array1";
        SampleSubclass ss1 = new SampleSubclass();
        ss1.value1 = "list_ss_1";
        ss1.value2 = "list_ss_1";
        SampleSubclass ss2 = new SampleSubclass();
        ss2.value1 = "list_ss_12";
        ss2.value2 = "list_ss_12";
        obj1.subclassList = new LinkedList<>();
        obj1.subclassList.add(ss1);
        obj1.subclassList.add(ss2);
        SampleClass obj2 = new SampleClass();
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
        obj2.subclassList.get(0).value1 = "list_ss_2";
        System.out.println("Obj1: " + obj1);
        System.out.println("Obj2: " + obj2);
    }
}