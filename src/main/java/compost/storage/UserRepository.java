package compost.storage;

import compost.model.SimpleUser;
import compost.storage.MongoUserRepository.RankedUser;
import java.util.Collection;
import java.util.List;
import org.telegram.telegrambots.meta.api.objects.User;

public interface UserRepository {

  void upsertUser(Long chatId, User telegramUser, boolean incrementMessageCount);

  SimpleUser getUser(Long chatId, Long userId);

  Collection<SimpleUser> getAllUsers(Long chatId);

  List<RankedUser> getTopUsers(Long chatId, int limit);
}
