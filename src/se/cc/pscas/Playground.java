package se.cc.pscas;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Playground {



    public static void main(String[] arg) throws ParseException {

        final AtomicReference<List<Person>> cachedPersons = new AtomicReference<List<Person>>();
        final AtomicReference<String> updated = new AtomicReference<String>();

        ReadXML readXML = null;
        try {
            readXML = new ReadXML();
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ReadXML throwed an exception");
        }


        if (readXML != null) {

            cachedPersons.set(readXML.getPersonList());
            updated.set(readXML.getUpdated());

        } else {

            System.out.println("readXML failed!!");
        }


        System.out.println("# " + cachedPersons.get().size() );

        /*
        BasicPasswordEncryptor textEncryptor = new BasicPasswordEncryptor();
        String encryptedPassword = "M7yEv5WaVQDBppmuPJfkQ0ibN078a9TL";


        if (textEncryptor.checkPassword("blahblah", encryptedPassword)) {
            System.out.println("Welcome to Jasypt");
        } else {
            System.out.println("Invalid password, access denied!");
        }

*/

    }

}
