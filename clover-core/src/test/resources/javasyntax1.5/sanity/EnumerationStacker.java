package sanity;

/** This is a simple class which takes an arbitrary set of Enumerations and
    "stacks them up" so that a single Enumeration can be used to return the
    contents of multiple enumerations.

    @author Alan Eliasen, eliasen@mindspring.com
*/
public class EnumerationStacker<T> implements java.util.Enumeration<T>
{
   /** A vector which contains the list of enumerations */
   private java.util.Vector<java.util.Enumeration<T>> vec;

   /** The index of the current Enumeration we're working with. */
   private int currentIdx;

   /** The currrent Enumeration object we're working with. */
   private java.util.Enumeration<T> currentEnum;
   
   /** Create a new, empty EnumerationStacker */
   public EnumerationStacker()
   {
      vec = null;
      currentIdx = 0;
      currentEnum = null;
   }

   /** Adds another enumeration to the list.  All enumerations should be added
       before calling any of the methods of <CODE>java.util.Enumeration</CODE>
   */
   public void addEnumeration(java.util.Enumeration<T> aEnum)
   {
      if (aEnum == null)
         return;
      
      if (vec == null)
      {
         currentEnum = aEnum;
         currentIdx = 0;
         vec = new java.util.Vector<java.util.Enumeration<T>>(2);
      }

      vec.addElement(aEnum);
   }

   /** Returns true if any of the remaining iterations have items left. */
   public boolean hasMoreElements()
   {
      if (currentEnum == null)
         return false;
      
      if (currentEnum.hasMoreElements())
         return true;
      else
      {
         nextEnumeration();
         return ((currentEnum != null) && (currentEnum.hasMoreElements()));
      }
   }

   /** Returns the next element, if one exists. */
   public T nextElement()
   {
      if (currentEnum == null)
         throw new java.util.NoSuchElementException("EnumerationStacker: Requested nonexistent element");
      
      if (currentEnum.hasMoreElements())
         return currentEnum.nextElement();

      // This enumeration is exhausted, go on to next which has remaining
      // elements.
      nextEnumeration();

      // if currentEnum is null, we're at the end.
      if (currentEnum == null)
         throw new java.util.NoSuchElementException("EnumerationStacker: Requested nonexistent element");
      else
         return currentEnum.nextElement();
   }

   /** Skips to the next available Enumeration.  When the method exits, the
       value of currentEnum will be a reference to the next enumeration with
       remaining elements, or null if no more elements exist. */
   private void nextEnumeration()
   {
      while ((currentEnum != null) && (! currentEnum.hasMoreElements()))
      {
         vec.setElementAt(null, currentIdx); // Allow cleanup
         currentIdx++;
         if (currentIdx >= vec.size())
         {
            // We're at the end of the vector of enumerations.
            currentEnum = null;
            vec = null;         // Allow cleanup
            return;
         }
         else
         {
            currentEnum = vec.elementAt(currentIdx);
            // Now loop again to see if thie enumeration has more elements.
         }
      }
   }
}
