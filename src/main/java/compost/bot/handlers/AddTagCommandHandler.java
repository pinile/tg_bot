package compost.bot.handlers;

import compost.annotation.BotCommandMapping;
import compost.annotation.LoggableCommand;
import compost.bot.CodeCompostInspectorBot;
import compost.service.TagService;
import compost.util.Constants.BotCommand;
import compost.util.MessageUtils;
import org.springframework.stereotype.Component;

@Component
@BotCommandMapping(BotCommand.ADDTAG)
public class AddTagCommandHandler implements CommandHandler {

  private final TagService tagService;
  private final MessageUtils messageUtils;

  public AddTagCommandHandler(TagService tagService, MessageUtils messageUtils) {
    this.tagService = tagService;
    this.messageUtils = messageUtils;
  }

  @Override
  @LoggableCommand
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    String message = tagService.buildAddTagResponse(context.chatId(), context.fullText());
    messageUtils.sendText(context.chatId(), context.threadId(), message);
  }
}
