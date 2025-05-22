package compost.bot.handlers;

import compost.bot.CodeCompostInspectorBot;

@FunctionalInterface
public interface CommandHandler {
  void handle(CodeCompostInspectorBot.CommandContext context);
}
