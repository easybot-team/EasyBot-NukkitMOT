package com.springwater.easybot.nukkitmot.command;

import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.lang.CommandOutputContainer;
import cn.nukkit.lang.TextContainer;
import cn.nukkit.lang.TranslationContainer;

public final class CapturingConsoleCommandSender extends ConsoleCommandSender {
    private final StringBuilder output = new StringBuilder();

    @Override
    public void sendMessage(String message) {
        append(message);
    }

    @Override
    public void sendMessage(TextContainer message) {
        append(getServer().getLanguage().translate(message));
    }

    @Override
    public void sendCommandOutput(CommandOutputContainer container) {
        container.getMessages().forEach(message -> append(getServer().getLanguage().translate(
                new TranslationContainer(message.getMessageId(), message.getParameters())
        )));
    }

    public String result(boolean success) {
        if (output.isEmpty()) {
            return success ? "命令执行成功" : "命令不存在或执行失败";
        }
        return output.toString();
    }

    private void append(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (!output.isEmpty()) {
            output.append('\n');
        }
        output.append(message.trim());
    }
}
