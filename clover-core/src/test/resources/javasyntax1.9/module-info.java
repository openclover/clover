import java9.internal.Internal;
import java9.spi.SPI;

@SuppressWarnings("abc" + "def" + (2 * 23))
module javasyntax {
    requires java.base;
    requires transitive java.xml;
    requires static java.sql;
    exports java9;
    exports java9.spi;
    exports java9.internal to javasyntax.internal, javasyntax.internal2;
    opens java9.api to common.spi, common.api;
    uses java.lang.String;
    provides SPI with Internal;
}