package org.openclover.idea.util;

import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class NetUtil {
    private NetUtil() {
    }

    /**
     * Open input stream for specified URL using <code>HttpConfigurable#openHttpConnection</code> with built-in proxy
     * support.
     *
     * @param url resource to be opened
     * @return InputStream or <code>null</code>
     * @throws IOException if network connection fails
     */
    @Nullable
    public static InputStream openUrlStream(@NotNull String url) throws IOException {
        final HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
        if (httpConfigurable != null) {
            // try HttpConfigurable.openConnection() (it might be via proxy)
            final URLConnection connection = httpConfigurable.openConnection(url);
            connection.connect();
            return connection.getInputStream();
        } else {
            // try direct connection
            final URL theUrl = new URL(url);
            return theUrl.openStream();
        }
    }

}
