package io.github.kosyakmakc.socialBridgeVk;

import java.io.StringReader;

import io.github.kosyakmakc.socialBridge.Commands.Arguments.ArgumentFormatException;
import io.github.kosyakmakc.socialBridge.Commands.Arguments.CommandArgument;
import io.github.kosyakmakc.socialBridge.Commands.SocialCommands.SocialCommandExecutionContext;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialMessage;
import io.github.kosyakmakc.socialBridge.SocialPlatforms.ISocialPlatform;

public class VkSocialCommandExecutionContext extends SocialCommandExecutionContext {
    private static final CommandArgument<String> systemWordArgument = CommandArgument.ofWord("/{botSuffix}-{commandLiteral} [arguments, ...]");
    private static final CommandArgument<String> systemGreedyStringArgument = CommandArgument.ofGreedyString("[arguments, ...]");

    private final String module;
    private final String command;
    private final String arguments;

    public VkSocialCommandExecutionContext(ISocialMessage message, String botUsername) {
        super(message);

        var fullMessageReader = new StringReader(getFullMessage());

        try {
            // pumping "/{moduleSuffix}-{commandLiteral}" in reader
            var commandLiteral = systemWordArgument.getValue(fullMessageReader);
            var otherText = systemGreedyStringArgument.getValue(fullMessageReader);

            if (botUsername != null && commandLiteral.endsWith('@' + botUsername)) {
                commandLiteral = commandLiteral.substring(0, commandLiteral.length() - botUsername.length() - 1);
            }

            if (commandLiteral.charAt(0) != '/') {
                throw new RuntimeException("Bad command text of SocialMessage, missed required start symbol '/'");
            }

            var separatorIndex = commandLiteral.indexOf('_');

            if (separatorIndex == -1) {
                module = commandLiteral.substring(1, commandLiteral.length());
                command = "";
            }
            else {

                module = commandLiteral.substring(1, separatorIndex);
                command = commandLiteral.substring(separatorIndex + 1, commandLiteral.length());
            }
                arguments = otherText;
        }
        catch (ArgumentFormatException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public String getFullMessage() {
        return getSocialMessage().getStringMessage();
    }

    @Override
    public String getMessage() {
        return arguments;
    }

    public String getModuleName() {
        return module;
    }

    public String getCommandLiteral() {
        return command;
    }

    public String getArguments() {
        return arguments;
    }

    @Override
    public ISocialPlatform getSocialPlatform() {
        return getSocialMessage().getSocialPlatform();
    }
}
