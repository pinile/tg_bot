package compost.storage;

import compost.model.SimpleUser;
import java.util.Collection;
import java.util.Map;
import org.telegram.telegrambots.meta.api.objects.User;

public interface UserRepository {

  void upsertUser(Long chatId, User telegramUser, boolean incrementMessageCount);

  SimpleUser getUser(Long chatId, Long userId);

  Collection<SimpleUser> getAllUsers(Long chatId);

  Map<SimpleUser, Integer> getTopUsers(Long chatId, int limit);
}