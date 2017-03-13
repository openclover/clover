public class TypeWhichChangesThrowsBehaviourAfterCompilation {
	public static void throwCheckedException(String message) throws ACheckedException {
         throw new ACheckedException(message);
	}

	static class ACheckedException extends Exception {
		public ACheckedException(String message) {
			super(message);
		}
	}
}