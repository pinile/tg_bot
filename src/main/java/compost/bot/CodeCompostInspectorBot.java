package compost.bot;

import compost.model.SimpleUser;
import compost.service.TagService;
import compost.service.TagService.TagResult;
import compost.service.UserService;
import compost.storage.MongoTagRepository;
import compost.storage.MongoUserRepository;
import compost.util.Constants;
import compost.util.Constants.TagOperationResult;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CodeCompostInspectorBot extends TelegramLongPollingBot {

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

  private static final Logger logger = LogManager.getLogger(CodeCompostInspectorBot.class);

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

      logger.debug("получено сообщение: fullText: {}, threadId: {}", fullText,
          threadId);

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        logger.debug("Групповое сообщение. Обработка пользователя: id={}, username={}",
            message.getFrom().getId(), message.getFrom().getUserName());
        userService.handleUser(chatId, message.getFrom(), !fullText.startsWith("/"));
      }

      // Проверка, является ли сообщение командой боту.
      if (fullText.startsWith("/")) {
        logger.debug("Обнаружена команда: {}", fullText);
        // Извлечение команды без параметров @.
        String rawCommand = fullText.split(" ")[0];
        String command = rawCommand.contains("@")
            ? rawCommand.substring(0, rawCommand.indexOf("@"))
            : rawCommand;

        logger.debug("Из сообщения извлечена команда: {}", command);
        logger.debug("Проверка threadId: полученный={}, ожидаемый={}", threadId,
            Constants.ALLOWED_THREAD_ID);

        // Проверка, что команда отправлена из разрешенной темы в группе (thread).
        if (!Objects.equals(threadId, Constants.ALLOWED_THREAD_ID)) {
          logger.debug("Команда из неверной темы. Отклонено.");
          // Если тема не верная, отправляем сообщение пользователю.
          SimpleUser su = userService.getUser(chatId, message.getFrom().getId());
          if (su != null) {
            logger.debug("Отправка уведомления пользователю о неверной теме.");
            messageUtils.sendText(chatId, Constants.ALLOWED_THREAD_ID,
                MessageBuilder.wrongThreadId(su));
          }
          return;
        }

        logger.debug("Команда {} проходит валидацию и будет исполнена", command);

        // Обработка комманд для бота.
        switch (command) {
          case "/help":
            sendHelp(chatId, threadId);
            break;
          case "/all":
            mentionAll(chatId, threadId);
            break;
          case "/tags":
            sendTags(chatId, threadId);
            break;
          case "/top":
            sendTop(chatId, threadId);
            break;
          case "/addtag":
            handleAddTag(chatId, threadId, fullText);
            break;
          case "/deltag":
            handleDeleteTag(chatId, threadId, fullText);
            break;
          case "/panic":
            messageUtils.sendText(chatId, threadId, MessageBuilder.enablePanic());
            break;
          default:
            messageUtils.sendText(chatId, threadId, MessageBuilder.unknownCommand());
        }
      }
    }
  }

  private void sendHelp(Long chatId, Integer threadId) {
    messageUtils.sendText(chatId, threadId, MessageBuilder.getHelp());
  }

  private void mentionAll(Long chatId, Integer threadId) {
    Collection<SimpleUser> users = userService.getAllUsers(chatId);
    if (users.isEmpty()) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.noUsersInChat());
      return;
    }
    messageUtils.sendText(chatId, threadId, MessageBuilder.mentionAll(users));
  }

  private void handleAddTag(Long chatId, Integer threadId, String fullText) {
    List<TagResult> results = tagService.tryAddTag(chatId, fullText);

    if (results == null || results.isEmpty()) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagException());
      return;
    }

    if (results.size() == 1
        && results.iterator().next().result() == TagOperationResult.INVALID_FORMAT) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.missingTagArg());
      return;
    }

    String message = MessageBuilder.addTagResults(results);
    messageUtils.sendText(chatId, threadId, message);
  }

  private void handleDeleteTag(Long chatId, Integer threadId, String fullText) {
    TagResult result = tagService.tryRemoveTag(chatId, fullText);
    switch (result.result()) {
      case INVALID_FORMAT ->
          messageUtils.sendText(chatId, threadId, MessageBuilder.invalidTagFormat());
      case TAG_NOT_FOUND ->
          messageUtils.sendText(chatId, threadId, MessageBuilder.tagNotFound(result.tag()));
      case SUCCESS ->
          messageUtils.sendText(chatId, threadId, MessageBuilder.tagDeleted(result.tag()));
      default -> messageUtils.sendText(chatId, threadId,
          MessageBuilder.tagException());
    }
  }

  private void sendTags(Long chatId, Integer threadId) {
    Map<String, String> tagMap = tagService.getTagMaps(chatId);

    // Сортировка по алфавиту: сначала теги с описанием, затем без.
    List<Map.Entry<String, String>> withDescription = tagMap.entrySet().stream()
        .filter(e -> e.getValue() != null && !e.getValue().isBlank())
        .sorted(Map.Entry.comparingByKey())
        .toList();

    List<String> withoutDescription = tagMap.entrySet().stream()
        .filter(e -> e.getValue() == null || e.getValue().isBlank())
        .map(Map.Entry::getKey)
        .sorted()
        .toList();

    String message = MessageBuilder.tagList(withDescription, withoutDescription);

    messageUtils.sendText(chatId, threadId, message);
  }

  private void sendTop(Long chatId, Integer threadId) {
    Map<SimpleUser, Integer> topUsers = userService.getTopUsers(chatId, 10);
    String message = MessageBuilder.topUsers(topUsers);
    messageUtils.sendText(chatId, threadId, message);
  }
}