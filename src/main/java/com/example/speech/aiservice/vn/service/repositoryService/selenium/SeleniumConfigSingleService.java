package com.example.speech.aiservice.vn.service.repositoryService.selenium;

import com.example.speech.aiservice.vn.model.entity.selenium.SeleniumConfigSingle;
import com.example.speech.aiservice.vn.model.repository.selenium.SeleniumConfigSingleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeleniumConfigSingleService {

    private final SeleniumConfigSingleRepository seleniumConfigSingleRepository;

    @Autowired
    public SeleniumConfigSingleService(SeleniumConfigSingleRepository seleniumConfigSingleRepository) {
        this.seleniumConfigSingleRepository = seleniumConfigSingleRepository;
    }

    public List<SeleniumConfigSingle> getAllConfigs() {
        return seleniumConfigSingleRepository.findAll();
    }

    public SeleniumConfigSingle saveConfig(SeleniumConfigSingle config) {
        return seleniumConfigSingleRepository.save(config);
    }

    public SeleniumConfigSingle getConfigByPort(String port) {
        return seleniumConfigSingleRepository.findByPort(port);
    }
}
