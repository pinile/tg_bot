package compost.bot.handlers;

import compost.annotation.BotCommandMapping;
import compost.annotation.LoggableCommand;
import compost.bot.CodeCompostInspectorBot;
import compost.service.TagService;
import compost.util.Constants.BotCommand;
import compost.util.MessageUtils;
import org.springframework.stereotype.Component;

@Component
@BotCommandMapping(BotCommand.TAGS)
public class TagsCommandHandler implements CommandHandler {

  private final TagService tagService;
  private final MessageUtils messageUtils;

  public TagsCommandHandler(TagService tagService, MessageUtils messageUtils) {
    this.tagService = tagService;
    this.messageUtils = messageUtils;
  }

  @Override
  @LoggableCommand
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    String message = tagService.getFormattedTagList(context.chatId());
    messageUtils.sendText(context.chatId(), context.threadId(), message);
  }
}
