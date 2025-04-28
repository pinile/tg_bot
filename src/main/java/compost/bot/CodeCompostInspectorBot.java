package compost.bot;

import compost.model.SimpleUser;
import compost.service.UserService;
import compost.storage.MongoUserRepository;
import compost.util.Constants;
import compost.util.MessageBuilder;
import compost.util.MessageUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  private final UserService userService;
  private final MessageUtils messageUtils;
  private final Set<String> tags = new HashSet<>();
  private final String botToken;

  public CodeCompostInspectorBot(String botToken) {
    this.botToken = botToken;
    this.messageUtils = new MessageUtils(this);
    this.userService = new UserService(new MongoUserRepository());
    loadTagsFromFile();
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
   * Обрабатывает входящие обновления (сообщения) от Telegram-бота. Метод проверяет тип сообщения,
   * выполняет команды и отправляет ответы пользователю.
   *
   * @param update Объект Update, содержащий информацию о входящем сообщении.
   */
  @Override
  public void onUpdateReceived(Update update) {
    // Проверка, что в сообщении есть текстовое сообщение.
    if (update.hasMessage() && update.getMessage().hasText()) {
      Message message = update.getMessage();
      Long chatId = message.getChatId();
      String fullText = message.getText().trim();
      Integer threadId = message.getMessageThreadId();

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        userService.handleUser(chatId, message.getFrom(), !fullText.startsWith("/"));
      }

      // Проверка, является ли сообщение командой боту.
      if (fullText.startsWith("/")) {
        // Извлечение команды без параметров @.
        String rawCommand = fullText.split(" ")[0];
        String command = rawCommand.contains("@")
            ? rawCommand.substring(0, rawCommand.indexOf("@"))
            : rawCommand;

        // Проверка, что команда отправлена из разрешенной темы в группе (thread).
        if (!Objects.equals(threadId, Constants.ALLOWED_THREAD_ID)) {
          // Если тема не верная, отправляем сообщение пользователю.
          SimpleUser su = userService.getUser(chatId, message.getFrom().getId());
          if (su != null) {
            messageUtils.sendText(chatId, Constants.ALLOWED_THREAD_ID,
                MessageBuilder.wrongThreadId(su));
          }
          return;
        }

        // Обработка комманд для бота.
        switch (command) {
          case "/help":
            messageUtils.sendText(chatId, threadId, MessageBuilder.getHelp());
            break;
          case "/all":
            mentionAll(chatId, threadId);
            break;
          case "/tags":
            messageUtils.sendText(chatId, threadId, MessageBuilder.tagList(tags));
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

  private void mentionAll(Long chatId, Integer threadId) {
    Collection<SimpleUser> users = userService.getAllUsers(chatId);
    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.noUsersInChat());
    }
    messageUtils.sendText(chatId, threadId, MessageBuilder.mentionAll(users));
  }

  private void loadTagsFromFile() {
    File file = new File(Constants.TAGS_FILE);
    if (!file.exists()) {
      return;
    }

    try (Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (!line.isEmpty()) {
          tags.add(line);
        }
      }
    } catch (IOException e) {
      System.out.println("Ошибка при чтении tags.txt: " + e.getMessage());
    }
  }

  private void saveTagsToFile() {
    try (PrintWriter writer = new PrintWriter(new File(Constants.TAGS_FILE))) {
      for (String tag : tags) {
        writer.println(tag);
      }
    } catch (IOException e) {
      System.out.println("Ошибка при записи tags.txt: " + e.getMessage());
    }
  }

  private void handleAddTag(Long chatId, Integer threadId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.missingTagArg());
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.invalidTagFormat());
      return;
    }

    if (tags.contains(tag)) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagExists(tag));
    } else {
      tags.add(tag);
      saveTagsToFile();
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagAdded(tag));
    }
  }

  private void handleDeleteTag(Long chatId, Integer threadId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.missingTagToDelete());
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.invalidTagFormat());
      return;
    }

    if (tags.remove(tag)) {
      saveTagsToFile();
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagDeleted(tag));
    } else {
      messageUtils.sendText(chatId, threadId, MessageBuilder.tagNotFound(tag));
    }
  }

  private void sendTop(Long chatId, Integer threadId) {
    Map<SimpleUser, Integer> topUsers = userService.getTopUsers(chatId, 10);
    String message = MessageBuilder.topUsers(topUsers);
    messageUtils.sendText(chatId, threadId, message);
  }
}