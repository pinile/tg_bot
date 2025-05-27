package compost.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Проверка метода pluralize")
@TestInstance(Lifecycle.PER_CLASS)
@Log4j2
public class PluralizationHelperTests {

  static Stream<Arguments> providePluralizeTestCases() {
    String baseWord = "сообщени";

    return Stream.of(
        Arguments.arguments("one (1)", 1, baseWord, "сообщение"),
        Arguments.arguments("one (21)", 21, baseWord, "сообщение"),
        Arguments.arguments("one (11)", 11, baseWord, "сообщений"),
        Arguments.arguments("few (2)", 2, baseWord, "сообщения"),
        Arguments.arguments("few (3)", 3, baseWord, "сообщения"),
        Arguments.arguments("few (12)", 12, baseWord, "сообщений"),
        Arguments.arguments("many (5)", 5, baseWord, "сообщений"),
        Arguments.arguments("many (20)", 20, baseWord, "сообщений"),
        Arguments.arguments("many (328)", 328, baseWord, "сообщений"),
        Arguments.arguments("zero (0)", 0, baseWord, "сообщений")
    );
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("providePluralizeTestCases")
  @DisplayName("Cклонение слова \"сообщение\"")
  void testPluralize(String description, int count, String base, String expected) {
    String result = PluralizationHelper.pluralize(count, base);
    log.info("Тест: {} -> pluralize({}, '{}') = {}", description, count, base, result);
    assertEquals(expected, result);
  }
}