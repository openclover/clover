package org.openclover.core.instr.java

import org.junit.Test

class InstrumentationTryStatementsTest extends InstrumentationTestBase {
    @Test
    void testTryResourceStatements() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A()) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A()) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A();) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A();) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A(); B b = new B()) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A(); RECORDER$AC0 CLR$ACI1=new RECORDER$AC0(){{RECORDER.inc(3);}};B b = new B()) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try(A a = new A(); B b = new B();) { } catch(Exception e) { } finally{ } }}",
                  "class B {$snifferField " + 'private void a(int arg) {RECORDER.inc(0); class RECORDER$AC0 implements java.lang.AutoCloseable {public void close(){}}; RECORDER.inc(1);try(RECORDER$AC0 CLR$ACI0=new RECORDER$AC0(){{RECORDER.inc(2);}};A a = new A(); RECORDER$AC0 CLR$ACI1=new RECORDER$AC0(){{RECORDER.inc(3);}};B b = new B();) { } catch(Exception e) { } finally{ } }}']
        ] as String[][])
    }

    @Test
    void testMultiCatchBlocks() throws Exception {
        checkInstrumentation([
                [ "class B { private void a(int arg) { try { } catch(FooException|BarException e) { } }}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0); RECORDER.inc(1);try { } catch(FooException|BarException e) { } }}"]
        ] as String[][])
        checkInstrumentation([
                [ "class B { private void a(int arg) { try { } catch(final FooException|BarException|BazException e) { } }}",
                  "class B {$snifferField private void a(int arg) {RECORDER.inc(0); RECORDER.inc(1);try { } catch(final FooException|BarException|BazException e) { } }}"]
        ] as String[][])
    }
}
