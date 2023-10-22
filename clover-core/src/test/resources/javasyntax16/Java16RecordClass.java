public class Java16RecordClass {
    public static void main(String[] args) {
        Record0 r0 = new Record0();
        Record1 r1 = new Record1(1, 2);
        Record2 r2 = new Record2(3, 4);
        Record3 r3 = new Record3(9, 9, 9);

        System.out.println(r0);
        System.out.println(r1);
        System.out.println(r2);
        System.out.println(r3);
        System.out.println(r3.sum());

        new RecordIsNotAReservedKeyword().record(10);
    }
}

/** An empty record */
record Record0() {}

/** A record with fields */
record Record1(int x, int y) {}

/** A recored with fields and constructor */
record Record2(int x, int y) {
    public Record2(int x, int y) {
        this.x = x * 2;
        this.y = y * 2;
    }
}

/** A record with extra methods; fields are not allowed */
record Record3(int x, int y, int z) {
    int sum() {
        return x + y + z;
    }
}

/** Despite introducing records in Java16, you can still use it for symbols, sic! */
class RecordIsNotAReservedKeyword {
    int record = 0;
    int record(int record) {
        this.record = record;
        System.out.println(record);
        return record;
    }
}

/** Test clashing of field modifiers and record modifiers */
final record R4(int a) { }

/** Test records inside classes */
class ClassWithRecords {
    // instance records, different visibility
    public record Pub(int a) { }
    record Pac(int a) { }
    protected record Pro(int a) { }
    private record Pri(int a) { }

    // 'static' is redundant but allowed
    static record Stat(int a) { }

    // 'final' is redundant but allowed
    final record Fin(int a) { }
}

/** Test records inside methods */
class MethodsWithRecords {
    public void records() {
        // inline records
        record Pac(int a) { }
        Pac p = new Pac(20);
        final int record = p.a();
    }
}

/** Test records with compact constructors */
record CompactConstructor(int a, int b) {
    CompactConstructor {
        a *= 10;
        b += 2;
    }
}