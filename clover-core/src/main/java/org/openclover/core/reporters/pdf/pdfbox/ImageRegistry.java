package org.openclover.core.reporters.pdf.pdfbox;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the images bundled with the reports and embeds each one into a document once, however many
 * pages draw it — the footer logo appears on every page but is stored a single time.
 *
 * <p>The counterpart of {@link FontRegistry} for images.
 */
class ImageRegistry {

    private static final int READ_BUFFER_SIZE = 8192;

    private final PDDocument document;
    private final Map<String, PDImageXObject> images = new HashMap<>();

    ImageRegistry(PDDocument document) {
        this.document = document;
    }

    /**
     * @param resourcePath classpath location of the image, e.g. {@code pdf_res/logo1.png}
     * @return the embedded image, loaded on first use
     * @throws IllegalStateException if the resource is missing or unreadable, which is a packaging
     *                               fault rather than a condition to render around
     */
    PDImageXObject get(String resourcePath) {
        return images.computeIfAbsent(resourcePath, path -> {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException(
                            "PDF report image not found on the classpath: " + path);
                }
                // the encoded bytes are handed over as they are, so PDFBox can embed the image
                // without a decode/re-encode round trip through ImageIO
                return PDImageXObject.createFromByteArray(document, readFully(in), path);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the PDF report image " + path, e);
            }
        });
    }

    /** @return how many distinct images have been embedded so far */
    int size() {
        return images.size();
    }

    private static byte[] readFully(InputStream in) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final byte[] chunk = new byte[READ_BUFFER_SIZE];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
