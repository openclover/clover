public class TypeWhichChangesThrowsBehaviourAfterCompilation {
	public static void throwCheckedException(String message) {
		//Class redefined in "version2" to throw MyException
	}
}