package com.atlassian.clover.api.command;

import java.util.List;

public class HelpBuilder {
    public static <C> String buildHelp(Class clazz,
                                       List<ArgProcessor<C>> mandatoryArgProcessors,
                                       List<ArgProcessor<C>> optionalArgProcessors) {
        final StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("  USAGE: ").append(clazz.getName()).append(" [OPTIONS] PARAMS\n\n");

        helpMessage.append("  PARAMS:\n");
        for (ArgProcessor argProcessor : mandatoryArgProcessors) {
            helpMessage.append(argProcessor.help());
        }

        helpMessage.append("  OPTIONS:\n");
        for (ArgProcessor argProcessor : optionalArgProcessors) {
            helpMessage.append(argProcessor.help());
        }

        return helpMessage.toString();
    }
}
