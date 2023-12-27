package demo.bot.service;

import demo.bot.dto.VacancyDto;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class VacancyService {
    private final Map<String, VacancyDto> vacancies = new HashMap<>();

    @PostConstruct
    public void init(){
        VacancyDto junMaDev = new VacancyDto();
        junMaDev.setId("1");
        junMaDev.setTitle("Junior Dev in Meta");
        junMaDev.setShortDescription("Java Core is required!");

        VacancyDto midGoDev = new VacancyDto();
        midGoDev.setId("2");
        midGoDev.setTitle("Middle Dev in Google");
        midGoDev.setShortDescription("Join our awesome team!");

        vacancies.put("1", junMaDev);
        vacancies.put("2", midGoDev);

    }



}
