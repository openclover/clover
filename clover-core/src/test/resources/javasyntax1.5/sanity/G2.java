package sanity;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class G2
{
    public void testDecls()
    {
        String v1;
        java.lang.String v2;
        int v3;
        float v4;
        int[] v5;
        String[] v6;
        float[][][] v7;
        
        // no nesting
        List<String> v8;
        Map<String,Integer> v9;
        Map3<String,Integer,Float> v10;
        
        Map3<java.lang.String,java.lang.Integer,java.lang.Float> v11;
        
        // two nestings
        List<List<String> > v12;
        List<List<String>> v13;
        Map<List<String>,Integer> v14;
        Map<Integer,List<String> > v15;
        Map<Integer,List<String>> v16;
        Map3<List<String>,Integer,Float> v17;
        Map3<Integer,List<String>,Float> v18;
        Map3<Integer,Float,List<String> > v19;
        Map3<Integer,Float,List<String>> v20;
        
        // three nestings
        List<List<List<String> > > v21;
        List<List<List<String>> > v22;
        List<List<List<String> >> v23;
        List<List<List<String>>> v24;
        Map<List<List<String> >,Integer> v25;
        Map<List<List<String>>,Integer> v26;
        Map<Integer,List<List<String> > > v27;
        Map<Integer,List<List<String> >> v28;
        Map<Integer,List<List<String>> > v29;
        Map<Integer,List<List<String>>> v30;
        Map3<List<List<String>>,Integer,Float> v31;
        Map3<Integer,List<List<String>>,Float> v32;
        Map3<Integer,Float,List<List<String> > > v33;
        Map3<Integer,Float,List<List<String>> > v34;
        Map3<Integer,Float,List<List<String> >> v35;
        Map3<Integer,Float,List<List<String>>> v36;
        
        // four nestings
        List<List<List<List<String> > > > v37;
        List<List<List<List<String>> >> v38;
        List<List<List<List<String>> > > v39;
        List<List<List<List<String> > >> v40;
        List<List<List<List<String> >>> v41;
        List<List<List<List<String>>> > v42;
        List<List<List<List<String>>>> v43;
        
        Map<List<List<List<String>>>,Integer> v44;
        Map<Integer,List<List<List<String> > > > v45;
        Map<Integer,List<List<List<String>> >> v46;
        Map<Integer,List<List<List<String>> > > v47;
        Map<Integer,List<List<List<String> > >> v48;
        Map<Integer,List<List<List<String>>> > v49;
        Map<Integer,List<List<List<String> >>> v50;
        
        // make sure more complicatd reference types can be
        // type arguments
        List<String[]> v51;
        List<List<String[]> > v52;
        List<List<String[]>> v53;
        
        Map<String,Integer> v54;
        Map<String,List<Integer> > v55;
        Map<String,List<Integer>> v56;
        Map<List<Integer>,String> v57;
        Map<List<Integer>,List<String>> v58;
        
        Map3<String,Integer,Float> v59;
        Map3<String,List<Integer>,Float> v60;
        
        Map<Map<String,String>,Map3<String,Integer,Float>> v61;
        Map<List<String>,List<Integer>> v62;
        Map3<List<String>,List<Integer>,List<Float>> v63;
        
//        List<Object>[] v64;
//        Map<String,List<Object>[]> v65;

        // composite names; generic classes that have inner classes
        Seq<String>.Zipper<Integer> v68;
//        Seq<String[]>.Zipper<Integer[][]>[] v69;
    }

    public void testExpressions() 
    {
        Object o = null;
        List<Integer> l1 = (ArrayList<Integer>)null;
        l1 = new ArrayList<Integer>();
        Map<Integer,String> l2 = null;
        
 //       List<Integer>[] a1 = new List<Integer>[1];
 //       a1[0] = l1;

        //
        // double end-angle-brackets
        //
        
        Map<String,List<Integer> > m1 = new HashMap<String,List<Integer> > ();
        Map<String,List<Integer>>m2 = new HashMap<String,List<Integer>>();

        Pair<Seq<Integer>,Seq<String>> p1 =
            new Pair<Seq<Integer>,Seq<String>>(
                new Seq<Integer>(new Integer(0), new Seq<Integer>()),
                new Seq<String>("abc", new Seq<String>()));

//       Seq<Character>[][][] sa1 = new Seq<Character>[10][20][];

        // casting
        l2 = (Map<Integer,String>) null;

        // instanceof
        boolean b1 = l2 instanceof Map;

        // cast or less-than operator:
        {
            class a<T>{};
            class b{};
            Object x = ( a < b > ) null;
        }
        {
            int a = 0;
            int b = 0;
            String x = ( a < b ) + "";
        }
    }
    
    /** a generic method */
    /* variance problem
    <Elem extends Comparable<Elem>> void sort(Elem[] a) {
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < i; j++)
                if (a[j].compareTo(a[i]) < 0) swap(a, i, j);
    }

    static <Elem> void swap(Elem[] a, int i, int j) {
        Elem temp = a[i]; a[i] = a[j]; a[j] = temp;
    }

    void foo()
    {
        // call a generic method
        String[] strings = {"a","c","d","z","b"};
        sort(strings);
    }
    */
    
}


class Pair<TA, TB>
{
    Pair(TA a, TB b) {}
    
}

class Pair1<TA extends java.lang.Number & Serializable,
           TB extends Serializable & Externalizable & Comparable<TA> > 
{
    TA a;
    TB b;

    static <E> void foo1(E x) {}
    static <E> E foo2(E x, E y) {return null;}
    static <E extends Exception> E foo2(E x, E y) throws E {return null;}
    
}

class Pair2<TA extends java.lang.Object & Serializable,
                       TB extends Serializable & Externalizable & Comparable<TA>>
{}


// two ending '>'
class Class1a<T1, T2 extends Comparator<T1>>{}
class Class1b<T1, T2 extends Comparator<T1> >{}
// three ending '>'
class Class2a<T1, T2 extends Comparator<List<T1>>>{}
class Class2b<T1, T2 extends Comparator<List<T1> >>{}
class Class2c<T1, T2 extends Comparator<List<T1>> >{}
class Class2d<T1, T2 extends Comparator<List<T1> > >{}
// four ending '>'
class Class3a<T1, T2 extends Comparator<Pair<String[],List<T1>>>>{}
class Class3b<T1, T2 extends Comparator<Pair<String[],List<T1>> >>{}
class Class3c<T1, T2 extends Comparator<Pair<String[],List<T1>> > >{}
class Class3d<T1, T2 extends Comparator<Pair<String[],List<T1> > >>{}
class Class3e<T1, T2 extends Comparator<Pair<String[],List<T1> >> >{}
class Class3f<T1, T2 extends Comparator<Pair<String[],List<T1>>> >{}
class Class3g<T1, T2 extends Comparator<Pair<String[],List<T1> >>>{}


// generic types in the extends and implements clauses of
// classes and interfaces
class BaseClass<T>{}
interface BaseInterface<T> {}
class SubClass1 extends BaseClass<Integer> {}
class SubClass2<T> extends BaseClass<T> {}
class SubClass3<T> extends BaseClass<T> implements BaseInterface<T>{}
class SubClass4 extends BaseClass<Integer> implements BaseInterface<String>{}
class SubClass5 extends BaseClass<List<Integer>> {}
class SubClass6 implements BaseInterface<List<String>>{}
interface BaseInterface2<T> extends BaseInterface<T>{}


// a class with three params (used in examples above)
class Map3<T1,T2,T3> {}


// copied from JSR14 document (but syntax errors fixed!
// those "this." expressions did not compile with ea compiler )
class Seq<A> {
    A head;
    Seq<A> tail;
    Seq() { this(null, null); }
    boolean isEmpty() { return tail == null; }
    Seq(A head, Seq<A> tail) { this.head = head; this.tail = tail; }
    class Zipper<B> {
        Seq<Pair<A,B>> zip(Seq<B> that) {
            if (isEmpty() || that.isEmpty())
                return new Seq<Pair<A,B>>();
            else
                return new Seq<Pair<A,B>>(
                    new Pair<A,B>(head, that.head),
                    zip(that.tail));
        }
        Seq<?> noop() {
            return null;
        }
    }
}

abstract class MyCopyOnWriteArraySet<E> extends AbstractSet<E>
        implements Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 5457747651344034263L;

    private final CopyOnWriteArrayList<E> al;

    /**
     * Constructs an empty set.
     */
    public MyCopyOnWriteArraySet() {
        al = new CopyOnWriteArrayList<E>();
    }

    /**
     * Constructs a set containing all of the elements of the specified
     * Collection.
     * @param c the collection
     */
    public <T extends E> MyCopyOnWriteArraySet(Collection<T> c) {
        al = new CopyOnWriteArrayList<E>();
        al.addAllAbsent(c);
    }


}
