package compost.bot;

import compost.model.SimpleUser;
import compost.service.TagService;
import compost.service.TagService.TagResult;
import compost.service.UserService;
import compost.storage.MongoTagRepository;
import compost.storage.MongoUserRepository;
import compost.storage.MongoUserRepository.RankedUser;
import compost.util.Constants;
import compost.util.Constants.BotCommand;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Log4j2
public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  public record CommandContext(
      Long chatId,
      Integer threadId,
      Message message,
      String fullText
  ) {

  }

  private final Map<BotCommand, Consumer<CommandContext>> handlers = Map.of(
      BotCommand.ADDTAG, this::handleAddTag,
      BotCommand.DELTAG, this::handleDeleteTag,
      BotCommand.HELP, this::sendHelp,
      BotCommand.ALL, this::mentionAll,
      BotCommand.TAGS, this::sendTags,
      BotCommand.TOP, this::sendTop,
      BotCommand.PANIC, this::sendPanic
  );

  private final UserService userService;
  private final MessageUtils messageUtils;
  private final String botToken;
  private final TagService tagService;

  public CodeCompostInspectorBot(String botToken) {
    this.botToken = botToken;
    this.messageUtils = new MessageUtils(this);
    this.userService = new UserService(new MongoUserRepository());
    this.tagService = new TagService(new MongoTagRepository());
  }

  @Override
  public String getBotUsername() {
    return "codeCompostInspectorBot";
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  /**
   * Метод для проверки различных типов контента в сообщении.
   *
   * @param message Объект message, содержащий сообщение пользователя.
   * @return Возвращает true, если сообщение содержит хотя бы один из типов контента, false, если
   * сообщение не содержит ни одного из них.
   */
  private boolean hasAnyContent(Message message) {
    if (message == null) {
      return false;
    }
    return message.hasText() ||
        message.hasPhoto() ||
        message.hasDocument() ||
        message.hasVideo() ||
        message.hasSticker() ||
        message.hasAudio() ||
        message.hasVoice() ||
        message.isReply();
  }

  /**
   * Обрабатывает входящие обновления (сообщения) от Telegram-бота. Метод проверяет тип сообщения,
   * выполняет команды и отправляет ответы пользователю.
   *
   * @param update Объект Update, содержащий информацию о входящем сообщении.
   */
  @Override
  public void onUpdateReceived(Update update) {
    // Проверка, что сообщение содержит допустимый контент.
    if (update.hasMessage() && hasAnyContent(update.getMessage())) {

      Message message = update.getMessage();
      Long chatId = message.getChatId();
      String fullText = message.hasText() ? message.getText().trim() : "";
      Integer threadId = message.getMessageThreadId();

      CommandContext context = new CommandContext(chatId, threadId, message, fullText);

      log.debug("Получено сообщение: fullText: {}, threadId: {}", fullText,
          threadId);

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        log.debug("Групповое сообщение. Обработка пользователя: id={}, username={}",
            message.getFrom().getId(), message.getFrom().getUserName());
        userService.handleUser(chatId, message.getFrom(), !fullText.startsWith("/"));
      }

      // Проверка, является ли сообщение командой боту.
      if (fullText.startsWith("/")) {
        log.debug("Обнаружена команда: {}", fullText);
        // Извлечение команды без параметров @.
        String rawCommand = fullText.split(" ")[0];
        String command = rawCommand.contains("@")
            ? rawCommand.substring(0, rawCommand.indexOf("@"))
            : rawCommand;

        log.debug("Из сообщения извлечена команда: {}", command);
        log.debug("Проверка threadId: полученный={}, ожидаемый={}", threadId,
            Constants.ALLOWED_THREAD_ID);

        // Проверка, что команда отправлена из разрешенной темы в группе (thread).
        if (!Objects.equals(threadId, Constants.ALLOWED_THREAD_ID)) {
          log.debug("Команда из неверной темы. Отклонено.");
          // Если тема не верная, отправляем сообщение пользователю.
          SimpleUser su = userService.getUser(chatId, message.getFrom().getId());
          if (su != null) {
            log.debug("Отправка уведомления пользователю о неверной теме.");
            messageUtils.sendText(chatId, Constants.ALLOWED_THREAD_ID,
                MessageBuilder.wrongThreadId(su));
          }
          return;
        }

        log.debug("Команда {} проходит валидацию и будет исполнена", command);

        BotCommand.fromString(command).ifPresentOrElse(botCommand -> {
          handlers.getOrDefault(botCommand, this::handleUnknownCommand).accept(context);
        }, () -> messageUtils.sendText(chatId, threadId, MessageBuilder.unknownCommand()));
      }
    }
  }

  private void handleUnknownCommand(CommandContext context) {
    messageUtils.sendText(context.chatId, context.threadId, MessageBuilder.unknownCommand());
  }

  private void sendHelp(CommandContext context) {
    messageUtils.sendText(context.chatId, context.threadId, MessageBuilder.getHelp());
  }

  private void sendPanic(CommandContext context) {
    messageUtils.sendText(context.chatId, context.threadId, MessageBuilder.enablePanic());
  }

  private void mentionAll(CommandContext context) {
    String message = userService.buildMentionAllMessage(context.chatId);
    messageUtils.sendText(context.chatId, context.threadId, message);
  }

  private void handleAddTag(CommandContext context) {
    String message = tagService.buildAddTagResponse(context.chatId, context.fullText);
    messageUtils.sendText(context.chatId, context.threadId, message);
  }

  private void handleDeleteTag(CommandContext context) {
    TagResult result = tagService.tryRemoveTag(context.chatId, context.fullText);
    switch (result.result()) {
      case INVALID_FORMAT -> messageUtils.sendText(context.chatId, context.threadId,
          MessageBuilder.invalidTagFormat());
      case TAG_NOT_FOUND -> messageUtils.sendText(context.chatId, context.threadId,
          MessageBuilder.tagNotFound(result.tag()));
      case SUCCESS -> messageUtils.sendText(context.chatId, context.threadId,
          MessageBuilder.tagDeleted(result.tag()));
      default -> messageUtils.sendText(context.chatId, context.threadId,
          MessageBuilder.tagException());
    }
  }

  private void sendTags(CommandContext context) {
    String message = tagService.getFormattedTagList(context.chatId);
    messageUtils.sendText(context.chatId, context.threadId, message);
  }

  private void sendTop(CommandContext context) {
    List<RankedUser> topUsers = userService.getTopUsers(context.chatId, 10);
    String message = MessageBuilder.topUsers(topUsers);
    messageUtils.sendText(context.chatId, context.threadId, message);
  }
}