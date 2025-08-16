package com.example.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Все тексты/кнопки/сообщения UI из bot-config.yml (prefix = ui)
 */
@Data
@ConfigurationProperties(prefix = "ui")
public class UiProperties {

    private MainMenu mainMenu;
    private ServicesMenu servicesMenu;
    private WalkMenu walkMenu;
    private Messages messages;
    private Faq faq;

    @Data
    public static class MainMenu {
        private String title;
        private Buttons buttons;

        @Data
        public static class Buttons {
            private String services;
            private String work;
            private String callManager;
            private String general;
        }
    }

    @Data
    public static class ServicesMenu {
        private String title;
        private Buttons buttons;

        @Data
        public static class Buttons {
            private String walk;
            private String boarding;
            private String nanny;
            private String back;
        }
    }

    @Data
    public static class WalkMenu {
        private String title;
        private Buttons buttons;

        @Data
        public static class Buttons {
            private String normal;
            private String active;
            private String cancel;
            private String back;
        }
    }

    @Data
    public static class Messages {
        private String draftHeader;
        private String draftTip;
        private String summary;
    }

    @Data
    public static class Faq {
        private String cost;
        private String pay;
        private String keys;
        private String medkit;
        private String washpaws;
        private String feed;
        private String contract;
    }
}