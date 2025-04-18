package compost;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

      if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
        saveUser(chatId, message.getFrom());
      }

      String fullText = message.getText().trim();
      if (fullText.startsWith("/")) {
        String rawCommand = fullText.split(" ")[0];
        String command = rawCommand.contains("@")
            ? rawCommand.substring(0, rawCommand.indexOf("@"))
            : rawCommand;

        switch (command) {
          case "/help":
            messageUtils.sendText(chatId, MessageBuilder.getHelp());
            break;
          case "/all":
            mentionAll(chatId);
            break;
          case "/tags":
            messageUtils.sendText(chatId, MessageBuilder.tagList(tags));
            break;
          case "/top":
            sendTop(chatId);
            break;
          case "/addtag":
            handleAddTag(chatId, fullText);
            break;
          case "/deltag":
            handleDeleteTag(chatId, fullText);
            break;
          case "/panic":
            messageUtils.sendText(chatId, MessageBuilder.enablePanic());
            break;
          default:
            messageUtils.sendText(chatId, MessageBuilder.unknownCommand());
        }
      }
      // если текст начинается не с / - ничего не делать, сохраняет пользователя.
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
    su.messageCount++; // счетчик сообщений
    usersChanged = true; // изменяем файл, если были обновления в течении шедулера (60 секунд)
  }

  private void mentionAll(Long chatId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, MessageBuilder.noUsersInChat());
    }
    messageUtils.sendText(chatId, MessageBuilder.mentionAll(users));
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

  private void handleAddTag(Long chatId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      messageUtils.sendText(chatId, MessageBuilder.missingTagArg());
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, MessageBuilder.invalidTagFormat());
      return;
    }

    if (tags.contains(tag)) {
      messageUtils.sendText(chatId, MessageBuilder.tagExists(tag));
    } else {
      tags.add(tag);
      saveTagsToFile();
      messageUtils.sendText(chatId, MessageBuilder.tagAdded(tag));
    }
  }

  private void handleDeleteTag(Long chatId, String fullText) {
    String[] parts = fullText.split(" ");
    if (parts.length < 2) {
      messageUtils.sendText(chatId, MessageBuilder.missingTagToDelete());
      return;
    }

    String tag = parts[1].trim();
    if (!tag.startsWith("#")) {
      messageUtils.sendText(chatId, MessageBuilder.invalidTagFormat());
      return;
    }

    if (tags.remove(tag)) {
      saveTagsToFile();
      messageUtils.sendText(chatId, MessageBuilder.tagDeleted(tag));
    } else {
      messageUtils.sendText(chatId, MessageBuilder.tagNotFound(tag));
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

  private void sendTop(Long chatId) {
    Map<Long, SimpleUser> users = groupUsers.get(chatId);
    if (users == null || users.isEmpty()) {
      messageUtils.sendText(chatId, MessageBuilder.noActiveUser());
      return;
    }

    List<SimpleUser> top = new ArrayList<>(users.values());
    top.sort((a, b) -> Integer.compare(b.messageCount, a.messageCount));
    messageUtils.sendText(chatId, MessageBuilder.topUsers(top, 5));
  }
}