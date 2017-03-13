package regression.CCD53;

public class TestCase 
{
    public static void main(String[] args) {
        Character ch = new Character('a');
        Class c = ch.getClass();
        if ((char.class.equals(c))) {
            System.out.println("class is character");
        }
        if ((char[].class.equals(c))) {
            System.out.println("class is character array");
        }

        Object attributeValue = "asd";
        if ((byte[].class.isInstance(attributeValue))) {
            System.out.println("blah");
        }
        
    }
    
}
