package com.example.speech.aiservice.vn.service.date;

import com.google.api.client.util.DateTime;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class Calculate {

    /**
     * Get the next publish time in UTC based on the given link:
     * - If the link contains "/xs/72/", return the next even hour (12, 14, 16, etc.) in Vietnam time
     * - Otherwise, return 18:00 Vietnam time (today or next day if already past 18:00)
     */
    public DateTime getNextVietnamPublishTimeUTC(String link) {
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(vietnamZone);

        if (link.contains("72136")) { // Chuyện phong thủy dân gian
            int currentHour = now.getHour();

            int nextEvenHour = ((currentHour + 1) / 2) * 2;

            if (now.getMinute() > 0 || now.getSecond() > 0 || now.getNano() > 0) {
                if (nextEvenHour <= currentHour) {
                    nextEvenHour += 2;
                }
            }

            if (nextEvenHour >= 24) {
                now = now.plusDays(1).withHour(nextEvenHour % 24).withMinute(0).withSecond(0).withNano(0);
            } else {
                now = now.withHour(nextEvenHour).withMinute(0).withSecond(0).withNano(0);
            }

            Instant utcInstant = now.withZoneSameInstant(ZoneOffset.UTC).toInstant();

            return new DateTime(utcInstant.toEpochMilli());
        } else if (link.contains("17456") || link.contains("97006") || link.contains("93809")) { // Chuyện xác chết và nhà minh và chuyện cô hàng xó
            ZonedDateTime targetTime = now.withHour(18).withMinute(0).withSecond(0).withNano(0);

            if (now.isAfter(targetTime)) {
                targetTime = targetTime.plusDays(1);
            }

            Instant utcInstant = targetTime.withZoneSameInstant(ZoneOffset.UTC).toInstant();
            return new DateTime(utcInstant.toEpochMilli());
        }
        return null;
    }
}
