package compost.util;

import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

/**
 * Утилита для склонения слов
 */
public final class PluralizationHelper {

  private static final PluralRules RUSSIAN_RULES =
      PluralRules.forLocale(ULocale.forLanguageTag("ru"));

  // Запрет на создание экземляров
  private PluralizationHelper() {
  }

  /**
   * Возвращает слово в правильной форме числа.
   *
   * @param count    Число (1, 2, 5...).
   * @param baseForm Основа слова (например, "сообщени" для "сообщение/сообщения...").
   */
  public static String pluralize(int count, String baseForm) {
    String form = RUSSIAN_RULES.select(count);
    return switch (form) {
      case "one" -> baseForm + "e";   // 1 сообщение
      case "few" -> baseForm + "s";   // 2 сообщения
      case "many" -> baseForm + "s";  // 5 сообщений
      default -> baseForm + "й";      // fallback
    };
  }
}