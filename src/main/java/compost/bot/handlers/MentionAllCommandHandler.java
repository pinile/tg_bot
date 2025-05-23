package compost.bot.handlers;

import compost.annotation.BotCommandMapping;
import compost.annotation.LoggableCommand;
import compost.bot.CodeCompostInspectorBot;
import compost.service.UserService;
import compost.util.Constants.BotCommand;
import compost.util.MessageUtils;
import org.springframework.stereotype.Component;

@Component
@BotCommandMapping(BotCommand.ALL)
public class MentionAllCommandHandler implements CommandHandler {

  private final UserService userService;
  private final MessageUtils messageUtils;

  public MentionAllCommandHandler(UserService userService, MessageUtils messageUtils) {
    this.userService = userService;
    this.messageUtils = messageUtils;
  }

  @Override
  @LoggableCommand
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    String message = userService.buildMentionAllMessage(context.chatId());
    messageUtils.sendText(context.chatId(), context.threadId(), message);
  }
}
