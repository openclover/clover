package org.openclover.core.reporters

import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class DonationMessageGeneratorTest {

    @Test
    void testAsTextEndsWithDonateLabel() {
        100.times {
            String text = DonationMessageGenerator.asText()
            assertTrue(text.endsWith(DonationMessageGenerator.DONATE_LABEL))
            assertTrue(text.length() > DonationMessageGenerator.DONATE_LABEL.length())
        }
    }

    @Test
    void testAsHtmlEndsWithDonateLink() {
        String expectedLink = "<a href=\"" + DonationMessageGenerator.DONATE_URL + "\">" +
                DonationMessageGenerator.DONATE_LABEL + "</a>"
        100.times {
            String html = DonationMessageGenerator.asHtml()
            assertTrue(html.endsWith(expectedLink))
            assertTrue(html.length() > expectedLink.length())
        }
    }

    @Test
    void testPickMessageReturnsMessageWithoutLink() {
        100.times {
            String message = DonationMessageGenerator.pickMessage()
            assertFalse(message.isEmpty())
            assertFalse(message.contains(DonationMessageGenerator.DONATE_URL))
            assertFalse(message.contains(DonationMessageGenerator.DONATE_LABEL))
        }
    }

    @Test
    void testMessagesAreRandomlyPicked() {
        Set<String> distinctMessages = new HashSet<>()
        200.times {
            distinctMessages.add(DonationMessageGenerator.pickMessage())
        }
        // with 200 draws we expect to have seen almost all available messages at least once
        int minExpected = Math.ceil(DonationMessageGenerator.messageCount() * 0.9d) as int
        assertTrue("Expected at least " + minExpected + " distinct donation messages out of " +
                DonationMessageGenerator.messageCount() + ", got: " + distinctMessages.size(),
                distinctMessages.size() >= minExpected)
    }
}
