package uz.uwon.pharm.utils;

import org.springframework.stereotype.Component;

@Component
public class PhoneFormatter {
    public String format(String phone) {
        if (phone == null || phone.length() != 12) return phone;

        return "+" + phone.substring(0, 3) +
                "-(" + phone.substring(3, 5) + ")" +
                "-" + phone.substring(5, 8) +
                "-" + phone.substring(8, 10) +
                "-" + phone.substring(10, 12);
    }

    public static String normalize(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() == 9 ? "998" + digits : digits;
    }
}
