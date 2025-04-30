package compost.bot;

import compost.model.SimpleUser;
import compost.service.UserService;
import compost.storage.MongoTagRepository;
import compost.storage.MongoUserRepository;
import compost.storage.TagRepository;
import compost.util.Constants;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  private final UserService userService;
  private final MessageUtils messageUtils;
  private final String botToken;
  private final TagRepository tagRepository = new MongoTagRepository();


  public CodeCompostInspectorBot(String botToken) {
    this.botToken = botToken;
    this.messageUtils = new MessageUtils(this);
    this.userService = new UserService(new MongoUserRepository());
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
    String[] parts = fullText.split(" ", 2);
    if (parts.length < 2) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.missingTagArg());
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.invalidTagFormat());
      return;
    }

    if (tagRepository.addTag(chatId, tag)) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagAdded(tag));
    } else {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagExists(tag));
    }
  }

  private void handleDeleteTag(Long chatId, Integer threadId, String fullText) {
    String[] parts = fullText.split(" ", 2);
    if (parts.length < 2) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.missingTagToDelete());
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.invalidTagFormat());
      return;
    }

    if (tagRepository.removeTag(chatId, tag)) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagDeleted(tag));
    } else {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagNotFound(tag));
    }
  }

  private void sendTags(Long chatId, Integer threadId) {
    Set<String> tagSet = tagRepository.getTags(chatId);
    messageUtils.sendText(chatId, threadId, MessageBuilder.tagList(tagSet));
  }

  private void sendTop(Long chatId, Integer threadId) {
    Map<SimpleUser, Integer> topUsers = userService.getTopUsers(chatId, 10);
    String message = MessageBuilder.topUsers(topUsers);
    messageUtils.sendText(chatId, threadId, message);
  }
}