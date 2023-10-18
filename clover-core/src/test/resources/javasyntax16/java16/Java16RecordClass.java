package java16;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Java16RecordClass {
    public void writeRecord() {
        Record1 r1 = new Record1(1, 2);
        System.out.println(r1);
        Record1 r2 = new Record1(3, 4);
        System.out.println(r2);
    }
}

record Record1(int x, int y) {}

record Record2(int x, int y) {
    public Record2(int x, int y) {
        this.x = x * 2;
        this.y = y * 2;
    }
}

/** Despite introducing records in Java16, you can still use it for symbols, sic! */
class RecordIsNotReservedKeyword {
    int record = 0;
    int record(int record) {
        this.record = record;
        System.out.println(record);
        return record;
    }
}