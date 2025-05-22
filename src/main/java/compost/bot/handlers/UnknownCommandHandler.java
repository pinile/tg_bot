package compost.bot.handlers;

import compost.bot.CodeCompostInspectorBot;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import org.springframework.stereotype.Component;

@Component
public class UnknownCommandHandler implements CommandHandler {

  private final MessageUtils messageUtils;

  public UnknownCommandHandler(MessageUtils messageUtils) {
    this.messageUtils = messageUtils;
  }

  @Override
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    messageUtils.sendText(context.chatId(), context.threadId(), MessageBuilder.unknownCommand());
  }
}
