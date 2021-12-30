package java9;

import java.io.OutputStream;
import java.io.PrintStream;

public class Java9TryWithResourcesWithVariable {
    public void tryWithResourcesWithVariable(final OutputStream os) {
        try (PrintStream ps = new PrintStream(os, true); os) {
            ps.println("write to stream");
        } catch (Exception e) {
        }
    }
}
