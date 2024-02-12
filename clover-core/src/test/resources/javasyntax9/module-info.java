// test: we must be able to parse expressions in annotations
@SuppressWarnings("abc" + "def" + (2 * 23))
module javasyntax {
    // test: parse all possible keywords
    requires java.base;
    requires transitive java.xml;
    requires static java.sql;
    exports java9;
    exports java9.spi;
    opens java9.api to common.spi, common.api, requires.api;
    uses java.lang.String;
    provides SPI with Internal, SpiImplOne, java9.internal.SpiImplTwo;

    // test: distinguish between keywords and identifiers
    exports exports;
    exports java9.internal to to, requires, transitive, exports, opens, uses, provides, with, java9.internal;
}