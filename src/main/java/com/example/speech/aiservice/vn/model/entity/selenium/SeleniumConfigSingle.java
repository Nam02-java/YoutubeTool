package com.example.speech.aiservice.vn.model.entity.selenium;

import jakarta.persistence.*;

@Entity
@Table(name = "selenium_config_single")
public class SeleniumConfigSingle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String port;

    @Column(name = "selenium_file_name", nullable = false)
    private String seleniumFileName;

    // Constructors
    public SeleniumConfigSingle() {}

    public SeleniumConfigSingle(String port, String seleniumFileName) {
        this.port = port;
        this.seleniumFileName = seleniumFileName;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getSeleniumFileName() {
        return seleniumFileName;
    }

    public void setSeleniumFileName(String seleniumFileName) {
        this.seleniumFileName = seleniumFileName;
    }
}
