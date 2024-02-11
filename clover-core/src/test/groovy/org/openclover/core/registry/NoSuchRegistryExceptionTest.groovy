package org.openclover.core.registry

import com.atlassian.clover.registry.NoSuchRegistryException
import org.junit.Test

import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Test for {@link NoSuchRegistryException}
 */
class NoSuchRegistryExceptionTest {

    @Test
    void testConstructorWithInitstring() {
        final NoSuchRegistryException exception = new NoSuchRegistryException("/path/to/clover.db")
        assertThat(exception.getMessage(),
                containsString("Clover registry file \"/path/to/clover.db\" does not exist, cannot be read or is a directory."))
    }

    @Test
    void testConstructorWithCustomMessage() {
        final File file = new File("clover.db")
        final NoSuchRegistryException exception = new NoSuchRegistryException(
                'Custom message ${file} is used', file)
        assertThat(exception.getMessage(),
                containsString("Custom message ${file.absolutePath} is used"))

    }
}
