package simple;

/**
 * @author Michael Studman
 */
public class ColumnAlignment
{
	private static class Inner
	{
		{ int i; i = 0; }
	}
    
	public static void main(String[] args)
	{
		int p = 10;
		if (p % 2 == 0) {
			p++;			
		} else {
			p--;
		}
	}
}
