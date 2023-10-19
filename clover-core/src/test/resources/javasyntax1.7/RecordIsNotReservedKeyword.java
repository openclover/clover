/**
 * Java 16 introduced records. In older java you can freely use it as symbols.
 * See https://github.com/openclover/clover/issues/206
 */
public class RecordIsNotReservedKeyword {
    int record = 0;
    int record(int record) {
        this.record = record;
        System.out.println(record);
        return record;
    }
}
