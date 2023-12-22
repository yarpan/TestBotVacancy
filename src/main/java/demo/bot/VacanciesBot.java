package demo.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;


@Component
public class VacanciesBot extends TelegramLongPollingBot {

    public VacanciesBot() {
        super(BotData.token);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.getMessage() != null) {
                handleStartCommand(update);
            }
            if (update.getCallbackQuery() != null) {
                String callbackData = update.getCallbackQuery().getData();
                if ("Show Junior vacancies".equals(callbackData)) {
                    showJuniorVacancies(update);
                } else if (callbackData.startsWith("vacancy_id=")) {
                    String id = callbackData.split("=")[1];
                    showVacancyDescription(id, update);
                }
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException("Can't send message to user", e);
        }
    }

    private void showVacancyDescription(String id, Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setText("Description of vacancy with ID=" + id);
        execute(sendMessage);

    }

    private void showJuniorVacancies(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy");
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setReplyMarkup(getJuniorVacanciesMenu());
        execute(sendMessage);
    }

    private ReplyKeyboard getJuniorVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton vacancy1 = new InlineKeyboardButton();
        vacancy1.setText("Junior Java Developer in Company1");
        vacancy1.setCallbackData("vacancy_id=1");
        row.add(vacancy1);

        InlineKeyboardButton vacancy2 = new InlineKeyboardButton();
        vacancy2.setText("Junior Python Developer in Company2");
        vacancy2.setCallbackData("vacancy_id=2");
        row.add(vacancy2);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }


    public void handleStartCommand(Update update) {
        String text = update.getMessage().getText();
        System.out.println("Received text: " + text);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("Welcome to vacancies Bot. Please choose your title:");
        sendMessage.setReplyMarkup(getStartMenu());
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private ReplyKeyboard getStartMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton junior = new InlineKeyboardButton();
        junior.setText("Junior");
        junior.setCallbackData("Show Junior vacancies");
        row.add(junior);

        InlineKeyboardButton middle = new InlineKeyboardButton();
        middle.setText("Middle");
        middle.setCallbackData("Show Middle vacancies");
        row.add(middle);

        InlineKeyboardButton senior = new InlineKeyboardButton();
        senior.setText("Senior");
        senior.setCallbackData("Show Senior vacancies");
        row.add(senior);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }


    @Override
    public String getBotUsername() {
        return "vsefayno.vacancies";
    }
}
