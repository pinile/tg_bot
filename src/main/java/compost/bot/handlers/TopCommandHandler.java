package compost.bot.handlers;

import compost.annotation.BotCommandMapping;
import compost.annotation.LoggableCommand;
import compost.bot.CodeCompostInspectorBot;
import compost.service.UserService;
import compost.storage.MongoUserRepository.RankedUser;
import compost.util.Constants.BotCommand;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@BotCommandMapping(BotCommand.TOP)
public class TopCommandHandler implements CommandHandler {

  private final UserService userService;
  private final MessageUtils messageUtils;

  public TopCommandHandler(UserService userService, MessageUtils messageUtils) {
    this.userService = userService;
    this.messageUtils = messageUtils;
  }

  @Override
  @LoggableCommand
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    List<RankedUser> topUsers = userService.getTopUsers(context.chatId(), 10);
    String message = MessageBuilder.topUsers(topUsers);
    messageUtils.sendText(context.chatId(), context.threadId(), message);
  }
}
