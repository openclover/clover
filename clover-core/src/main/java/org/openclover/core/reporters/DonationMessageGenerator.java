package org.openclover.core.reporters;

import java.util.Random;

public class DonationMessageGenerator {

    public static final String DONATE_URL = "https://openclover.org/donate";
    public static final String DONATE_LABEL = "☕openclover.org/donate";

    private static final String TEXT_LINK = DONATE_LABEL;
    private static final String HTML_LINK = "<a href=\"" + DONATE_URL + "\">" + DONATE_LABEL + "</a>";

    private static final String[] MESSAGES = new String[]{
            // Reciprocity / debt
            "This tool probably saved you 64 hours this year. A small coffee would help. ",

            // Humor / absurd
            "A programmer without coffee is just a very expensive compiler-error generator. ",

            // Scarcity / urgency (micro-time)
            "60 seconds. One click. No sign-up. Less time than it took to read this sentence. ",

            // Concrete numbers (anchoring)
            "Less than 1% of users fund 100% of development. The math isn't great — but it could change. ",

            // Engagement / co-ownership
            "You're not just a user. You're the crew keeping this project afloat. ",
            "Every coffee is a commit to a project that helps thousands every day. ",

            // Loss / FOMO
            "Open source projects don't die with a bang. They die from one missing coffee a month. ",
            "This tool survives exactly as long as someone feels like feeding it caffeine. ",

            // Social proof
            "Join the small group of people who figured out that 'free software' isn't 'costless'. ",

            // Direct / honest
            "You don't have to. But if you've ever thought 'damn, this is convenient' — this is that moment. ",

            // Personification / narrative
            "This project doesn't sleep at night. Neither do we — coffee helps a little. ",
            "Behind every line of code is someone who is wondering if there's room for a third coffee. ",
    };

    public static String asText() {
        return pickMessage() + TEXT_LINK;
    }

    public static String asHtml() {
        return pickMessage() + HTML_LINK;
    }

    /**
     * Returns a randomly picked donation message, without the trailing link/label.
     * Useful for callers (e.g. PDF rendering) that need to render the link themselves.
     */
    public static String pickMessage() {
        final Random rand = new Random();
        return MESSAGES[rand.nextInt(MESSAGES.length)];
    }

    public static int messageCount() {
        return MESSAGES.length;
    }
}

