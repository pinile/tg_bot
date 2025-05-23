package compost.bot.handlers;

import compost.annotation.BotCommandMapping;
import compost.annotation.LoggableCommand;
import compost.bot.CodeCompostInspectorBot;
import compost.service.TagService;
import compost.service.TagService.TagResult;
import compost.util.Constants.BotCommand;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import org.springframework.stereotype.Component;

@Component
@BotCommandMapping(BotCommand.DELTAG)
public class DeleteTagCommandHandler implements CommandHandler {

  private final TagService tagService;
  private final MessageUtils messageUtils;

  public DeleteTagCommandHandler(TagService tagService, MessageUtils messageUtils) {
    this.tagService = tagService;
    this.messageUtils = messageUtils;
  }

  @Override
  @LoggableCommand
  public void handle(CodeCompostInspectorBot.CommandContext context) {
    TagResult result = tagService.tryRemoveTag(context.chatId(), context.fullText());
    switch (result.result()) {
      case INVALID_FORMAT ->
          messageUtils.sendText(
              context.chatId(), context.threadId(), MessageBuilder.invalidTagFormat());
      case TAG_NOT_FOUND ->
          messageUtils.sendText(
              context.chatId(), context.threadId(), MessageBuilder.tagNotFound(result.tag()));
      case SUCCESS ->
          messageUtils.sendText(
              context.chatId(), context.threadId(), MessageBuilder.tagDeleted(result.tag()));
      default ->
          messageUtils.sendText(
              context.chatId(), context.threadId(), MessageBuilder.tagException());
    }
  }
}
