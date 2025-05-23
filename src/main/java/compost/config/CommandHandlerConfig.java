package compost.config;

import compost.annotation.BotCommandMapping;
import compost.bot.handlers.CommandHandler;
import compost.util.Constants.BotCommand;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class CommandHandlerConfig {

  private final ApplicationContext context;

  public CommandHandlerConfig(ApplicationContext context) {
    this.context = context;
  }

  @Bean
  public Map<BotCommand, CommandHandler> commandHandlerMap() {
    Map<BotCommand, CommandHandler> map = new HashMap<>();

    Map<String, CommandHandler> beans = context.getBeansOfType(CommandHandler.class);
    for (CommandHandler handler : beans.values()) {
      Class<?> targetClass = AopUtils.getTargetClass(handler);
      BotCommandMapping annotation = targetClass.getAnnotation(BotCommandMapping.class);
      if (annotation != null) {
        map.put(annotation.value(), handler);
        log.debug("Зарегистрированный handler для команды: {}", annotation.value());
      } else {
        log.warn("CommandHandler {} не имеет @BotCommandMapping аннотации.", targetClass);
      }
    }

    return map;
  }
}
