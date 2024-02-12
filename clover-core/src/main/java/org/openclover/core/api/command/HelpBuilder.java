package org.openclover.core.api.command;

import java.util.List;

public class HelpBuilder {
    public static <C> String buildHelp(Class<?> clazz,
                                       List<ArgProcessor<C>> mandatoryArgProcessors,
                                       List<ArgProcessor<C>> optionalArgProcessors) {
        final StringBuilder helpMessage = new StringBuilder();
        helpMessage.append("  USAGE: ").append(clazz.getName()).append(" [OPTIONS] PARAMS\n\n");

        helpMessage.append("  PARAMS:\n");
        for (ArgProcessor<C> argProcessor : mandatoryArgProcessors) {
            helpMessage.append(argProcessor.help() + "\n");
        }

        helpMessage.append("  OPTIONS:\n");
        for (ArgProcessor<C> argProcessor : optionalArgProcessors) {
            helpMessage.append(argProcessor.help() + "\n");
        }

        return helpMessage.toString();
    }
}
