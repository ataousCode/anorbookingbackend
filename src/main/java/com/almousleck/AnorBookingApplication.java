package com.almousleck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AnorBookingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnorBookingApplication.class, args);
    }
    //https://claude.ai/referral/aJCrVC0_gg
}
