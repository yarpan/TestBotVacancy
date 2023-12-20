package vacancy.bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;


@Component
public class VacanciesBot extends TelegramLongPollingBot {

    public VacanciesBot() {
        super(BotData.token);
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Event received");
    }

    @Override
    public String getBotUsername() {
        return "vsefayno.vacancies";
    }
}
