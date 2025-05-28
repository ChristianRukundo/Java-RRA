package rca.ac.rw.template.utils;

import org.springframework.stereotype.Component;

@Component
public class Utility {



    private String randomLetter() {
        char[] letters = {'A', 'B', 'C', 'D', 'E', 'F'};
        int index = (int) (Math.random() * letters.length);
        return String.valueOf(letters[index]);
    }


    private String randomDigits(int count) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < count; i++) {
            digits.append((int) (Math.random() * 10));
        }
        return digits.toString();
    }
}
