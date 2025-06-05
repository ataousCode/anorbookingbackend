package com.almousleck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AnorBookingApplication {
    // docker exec -it anor-booking-postgres psql -U anor_user -d anor_booking
    public static void main(String[] args) {
        SpringApplication.run(AnorBookingApplication.class, args);
    }
    //https://claude.ai/referral/aJCrVC0_gg
}
