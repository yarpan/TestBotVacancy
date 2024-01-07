package demo.bot;

import demo.bot.data.BotData;
import demo.bot.dto.VacancyDto;
import demo.bot.service.VacancyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;*/


@Component
public class VacanciesBot extends TelegramLongPollingBot {
    @Autowired
    private VacancyService vacancyService;
    private final Map<Long, String> lastShownVacancyLevel = new HashMap<>();
    private WebClient webClient = WebClient.create("https://api.openai.com/v1/chat/completions");
    public List<String> levels = List.of("Junior", "Middle", "Senior");
    public Map<String, List<String>> levelMap = new HashMap<>();


    public VacanciesBot() {
        super(BotData.TELEGRAM_TOKEN);
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.getMessage() != null) {
                handleStartCommand(update);
            }
            if (update.getCallbackQuery() != null) {
                String callbackData = update.getCallbackQuery().getData();
                if (callbackData.startsWith("show") && callbackData.endsWith("Vacancies")) {
                    switch (callbackData) {
                        case "showJuniorVacancies" -> showLevelVacancies("Junior", update);
                        case "showMiddleVacancies" -> showLevelVacancies("Middle", update);
                        case "showSeniorVacancies" -> showLevelVacancies("Senior", update);
                    }
                } else if (callbackData.startsWith("vacancyId=")) {
                    String id = callbackData.split("=")[1];
                    showVacancyDescription(id, update);
                } else if ("backToVacancies".equalsIgnoreCase(callbackData)) {
                    handleBackToVacanciesCommand(update);
                } else if ("backToStart".equalsIgnoreCase(callbackData)) {
                    handleBackToStartCommand(update);
                } else if ("getCoverLetter".equalsIgnoreCase(callbackData)) {
					handleGetCoverLetterCommand(update);
				}
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException("Can't send message to user!", e);
        }
    }


    private void handleGetCoverLetterCommand(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
		/*String text = update.getCallbackQuery().getMessage().getText();
		sendMessage.setText("Your cover letter:\n" + generateCoverLetter(text));*/
        execute(sendMessage);
    }


    private String generateCoverLetter(String vacancyDescription) {
        String coverLetter = "";
        Mono<String> response = webClient.post()
                .header("Content-Type", "application/json")
                .header("Authorization", BotData.OPENAI_API_KEY)
                .bodyValue("{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"Write a cover letter for vacancy. I am a Java developer with 6 months of experience, I know Java Core well, I know how to work with a database and create web applications. I have good knowledge of Hibernate and String Boot frameworks. Make cover letter not informal and without contact data and address. Vacancy: " + vacancyDescription + "\"}]}")
                .retrieve()
                .bodyToMono(String.class);
        coverLetter = response.block();
        return coverLetter;
    }


    public void handleStartCommand(Update update) throws TelegramApiException {
        String text = update.getMessage().getText();
        System.out.println("Received text: " + text);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("Welcome to vacancies Bot. Please choose your title:");
        sendMessage.setReplyMarkup(getStartMenu());
        execute(sendMessage);
    }


    private void handleBackToStartCommand(Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Choose Title:");
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setReplyMarkup(getStartMenu());
        execute(sendMessage);
    }


    private void handleBackToVacanciesCommand(Update update) throws TelegramApiException {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String level = lastShownVacancyLevel.get(chatId);
        showLevelVacancies(level, update);
    }

    private void showVacancyDescription(String id, Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        VacancyDto vacancy = vacancyService.get(id);
		/*StringBuilder text = new StringBuilder("Title: ");
		text.append(vacancy.getTitle()).append("\nCompany: ").append(vacancy.getCompany())
				.append("\nShort Description: ").append(vacancy.getShortDescription())
				.append("\nLong Description: ").append(vacancy.getLongDescription())
				.append("\nSalary: ").append(vacancy.getSalary())
				.append("\nLink: Click here to get more information: ").append(vacancy.getLink());*/
        String vacancyInfo = """
                *Title:* %s
                *Company:* %s
                *Short Description:* %s
                *Long Description:* %s
                *Salary:* %s
                *Link:* [%s](%s)
                """.formatted(
                escapeMarkdownReservedChars(vacancy.getTitle()),
                escapeMarkdownReservedChars(vacancy.getCompany()),
                escapeMarkdownReservedChars(vacancy.getShortDescription()),
                escapeMarkdownReservedChars(vacancy.getLongDescription()),
                vacancy.getSalary().isBlank() || vacancy.getSalary().equals("-") ? "Not specified" : escapeMarkdownReservedChars(vacancy.getSalary()),
                "Click here to get details",
                escapeMarkdownReservedChars(vacancy.getLink()));
        sendMessage.setText(vacancyInfo);
        sendMessage.setParseMode(ParseMode.MARKDOWNV2);
        sendMessage.setReplyMarkup(getBackToVacanciesMenu());
        execute(sendMessage);
    }

    private String escapeMarkdownReservedChars(String text) {
        return text.replace("-", "\\-")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }


    private ReplyKeyboard getBackToVacanciesMenu() {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton backToVacanciesButton = new InlineKeyboardButton();
        backToVacanciesButton.setText("All Vacancies");
        backToVacanciesButton.setCallbackData("backToVacancies");
        row.add(backToVacanciesButton);

        InlineKeyboardButton backToStartMenuButton = new InlineKeyboardButton();
        backToStartMenuButton.setText("Start Menu");
        backToStartMenuButton.setCallbackData("backToStart");
        row.add(backToStartMenuButton);

        InlineKeyboardButton getChatGptButton = new InlineKeyboardButton();
        getChatGptButton.setText("Get cover letter");
        getChatGptButton.setUrl("https://chat.openai.com/");
        //getChartGptButton.setCallbackData("getCoverLetter");
        row.add(getChatGptButton);

        //return new InlineKeyboardMarkup(List.of(row));
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }

    private void showLevelVacancies(String level, Update update) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Please choose vacancy:");
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setReplyMarkup(getLevelVacanciesMenu(level));
        execute(sendMessage);
        lastShownVacancyLevel.put(chatId, level);
    }


    private ReplyKeyboard getLevelVacanciesMenu(String level) {
        List<InlineKeyboardButton> raw = new ArrayList<>();
        List<VacancyDto> vacancies = vacancyService.getLevelVacancies(level);
        for (VacancyDto vacancy : vacancies) {
            InlineKeyboardButton vacancyButton = new InlineKeyboardButton();
            vacancyButton.setText(vacancy.getTitle());
            vacancyButton.setCallbackData("vacancyId=" + vacancy.getId());
            raw.add(vacancyButton);
        }
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(raw));
        return keyboard;
    }


    private ReplyKeyboard getVacanciesMenu(List<VacancyDto> vacancies) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (VacancyDto vacancy : vacancies) {
            InlineKeyboardButton vacancyButton = new InlineKeyboardButton();
            vacancyButton.setText(vacancy.getTitle());
            vacancyButton.setCallbackData("vacancyId=" + vacancy.getId());
            row.add(vacancyButton);
        }
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }


    private ReplyKeyboard getStartMenu() {
        List<InlineKeyboardButton> raw = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(levels.get(i));
            button.setCallbackData("show" + levels.get(i) + "Vacancies");
            raw.add(button);
        }
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(List.of(raw));
        return keyboard;
    }


    @Override
    public String getBotUsername() {
        return "vsefayno.vacancies";
    }
}
