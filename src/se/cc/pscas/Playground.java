package se.cc.pscas;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;

public class Playground {


    public static void main(String[] arg) {


        BasicPasswordEncryptor textEncryptor = new BasicPasswordEncryptor();
        String encryptedPassword = "M7yEv5WaVQDBppmuPJfkQ0ibN078a9TL";


        if (textEncryptor.checkPassword("blahblah", encryptedPassword)) {
            System.out.println("Welcome to Jasypt");
        } else {
            System.out.println("Invalid password, access denied!");
        }

    }

}
