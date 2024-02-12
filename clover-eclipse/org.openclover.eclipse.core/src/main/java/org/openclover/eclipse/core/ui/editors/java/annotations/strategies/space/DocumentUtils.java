package org.openclover.eclipse.core.ui.editors.java.annotations.strategies.space;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public class DocumentUtils {
    public static int lineColToOffset(IDocument document, int line, int col) throws BadLocationException {
        IRegion lineRegion = document.getLineInformation(line - 1);
        return lineRegion.getOffset() + col - 1;
    }
}
