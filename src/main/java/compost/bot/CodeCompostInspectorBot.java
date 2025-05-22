package compost.bot;

import com.mongodb.client.MongoDatabase;
import compost.bot.handlers.CommandHandler;
import compost.bot.handlers.UnknownCommandHandler;
import compost.model.SimpleUser;
import compost.service.TagService;
import compost.service.UserService;
import compost.storage.MongoTagRepository;
import compost.storage.MongoUserRepository;
import compost.util.Constants;
import compost.util.Constants.BotCommand;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.util.Map;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Log4j2
public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  public record CommandContext(
      Long chatId,
      Integer threadId,
      Message message,
      String fullText
  ) {

  }

  private final Map<Constants.BotCommand, CommandHandler> handlers;
  private final UserService userService;
  private final String botToken;
  private final TagService tagService;
  private final ApplicationContext applicationContext;
  private final MessageUtils messageUtils;
  private final UnknownCommandHandler unknownCommandHandler;

  @Autowired
  public CodeCompostInspectorBot(@Value("${bot.token}") String botToken,
      MongoDatabase mongoDatabase,
      ApplicationContext applicationContext,
      Map<Constants.BotCommand, CommandHandler> handlers,
      MessageUtils messageUtils,
      UnknownCommandHandler unknownCommandHandler) {
    this.botToken = botToken;
    this.applicationContext = applicationContext;
    this.handlers = handlers;
    this.userService = new UserService(new MongoUserRepository(mongoDatabase));
    this.tagService = new TagService(new MongoTagRepository(mongoDatabase));
    this.messageUtils = messageUtils;
    this.unknownCommandHandler = unknownCommandHandler;
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

  public Message sendMethod(BotApiMethod<Message> method) throws TelegramApiException {
    return super.execute(method);
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

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
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

        // Проверка, что команда отправлена из разрешенной темы в группе (thread).
        if (!Objects.equals(threadId, Constants.ALLOWED_THREAD_ID)) {

          SimpleUser su = userService.getUser(chatId, message.getFrom().getId());
          if (su != null) {
            messageUtils.sendText(chatId, Constants.ALLOWED_THREAD_ID,
                MessageBuilder.wrongThreadId(su));
          }
          return;
        }

        BotCommand.fromString(command).ifPresentOrElse(
            botCommand -> handlers.getOrDefault(botCommand, unknownCommandHandler).handle(context),
            () -> unknownCommandHandler.handle(context)
        );
      }
    }
  }
}