package compost.bot.handlers;

import compost.annotation.BotCommandMapping;
import compost.annotation.LoggableCommand;
import compost.bot.CodeCompostInspectorBot;
import compost.util.Constants.BotCommand;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import org.springframework.stereotype.Component;

@Component
@BotCommandMapping(BotCommand.HELP)
public class HelpCommandHandler implements CommandHandler {

  private final MessageUtils messageUtils;

  public HelpCommandHandler(MessageUtils messageUtils) {
    this.messageUtils = messageUtils;
  }

  @Override
  @LoggableCommand
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    messageUtils.sendText(context.chatId(), context.threadId(), MessageBuilder.getHelp());
  }
}
