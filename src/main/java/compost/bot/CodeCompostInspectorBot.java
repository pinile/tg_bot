package compost.bot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import compost.Constants;
import compost.model.SimpleUser;
import java.io.PrintWriter;
import java.util.concurrent.ScheduledExecutorService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.*;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CodeCompostInspectorBot extends TelegramLongPollingBot {

  private final MessageUtils messageUtils;
  private final Set<String> tags = new HashSet<>();
  private final Map<Long, Map<Long, SimpleUser>> groupUsers = new HashMap<>();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String botToken;
  private boolean usersChanged = false; // смотрит изменения по пользователю
  private static final String STORAGE_FILE = "users.json";
  private static final String TAGS_FILE = "tags.txt";

  public CodeCompostInspectorBot(String botToken) {
    this.botToken = botToken;
    this.messageUtils = new MessageUtils(this);
    loadUsersFromFile();
    loadTagsFromFile();

    // шедулер для сохранения users.json раз в минуту
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::saveUsersToFile, 60, 60, TimeUnit.SECONDS);
  }

  @Override
  public String getBotUsername() {
    return "codeCompostInspectorBot";
  }

  @Override
  public String getBotToken() {
    return botToken;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      Message message = update.getMessage();
      Long chatId = message.getChatId();

      Integer threadId = message.getMessageThreadId(); // Id чата в группе, может быть null
      System.out.println("Thread ID: " + message.getMessageThreadId()); // log

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        saveUser(chatId, message.getFrom());
      }

      String fullText = message.getText().trim();
      if (fullText.startsWith("/")) {
        String rawCommand = fullText.split(" ")[0];
        String command = rawCommand.contains("@")
            ? rawCommand.substring(0, rawCommand.indexOf("@"))
            : rawCommand;

        if (!Objects.equals(threadId, Constants.ALLOWED_THREAD_ID)) {
          Map<Long, SimpleUser> users = groupUsers.get(chatId);
          if (users != null) {
            SimpleUser su = users.get(message.getFrom().getId());
            if (su != null) {
              messageUtils.sendText(chatId, Constants.ALLOWED_THREAD_ID, MessageBuilder.wrongThreadId(su));
            }
          }
          return;
        }

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
      // если текст начинается не с / - ничего не делать, но сохранять пользователя.
    }
  }

  private void saveUser(Long chatId, User user) {
    groupUsers.putIfAbsent(chatId, new HashMap<>());
    Map<Long, SimpleUser> chatMap = groupUsers.get(chatId);

    SimpleUser su = chatMap.get(user.getId());
    if (su == null) {
      su = new SimpleUser(user);
      chatMap.put(user.getId(), su);
    }
    su.incrementMessageCount(); // счетчик сообщений
    usersChanged = true; // изменяем файл, если были обновления в течении шедулера (60 секунд)
  }

  private void mentionAll(Long chatId, Integer threadId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.noUsersInChat());
    }
    messageUtils.sendText(chatId, threadId,MessageBuilder.mentionAll(users));
  }

  private void loadUsersFromFile() {
    try {
      File file = new File(STORAGE_FILE);
      if (file.exists()) {
        Map<Long, Map<Long, SimpleUser>> data = mapper.readValue(file, new TypeReference<>() {
        });
        groupUsers.putAll(data);
      }
    } catch (IOException e) {
      System.out.println("Ошибка при загрузке навозников: " + e.getMessage());
    }
  }

  private void loadTagsFromFile() {
    File file = new File(TAGS_FILE);
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
    try (PrintWriter writer = new PrintWriter(new File(TAGS_FILE))) {
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

  private void saveUsersToFile() {
    if (!usersChanged) {
      return; // не сохраняем если ничего не менялось
    }

    try {
      mapper.writerWithDefaultPrettyPrinter().writeValue(new File(STORAGE_FILE), groupUsers);
      usersChanged = false; // после успешного сохранения сбрасываем флаг
    } catch (IOException e) {
      System.out.println("Ошибка при сохранении навозников: " + e.getMessage());
    }
  }

  private void sendTop(Long chatId, Integer threadId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, threadId, MessageBuilder.noActiveUser());
      return;
    }

    List<SimpleUser> top = new ArrayList<>(users.values());
    top.sort((a, b) -> Integer.compare(b.getMessageCount(), a.getMessageCount()));
    messageUtils.sendText(chatId, threadId, MessageBuilder.topUsers(top, 10));
  }
}