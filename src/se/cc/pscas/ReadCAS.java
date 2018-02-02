package se.cc.pscas;

/**
 * Created by crco0001 on 2/2/2018.
 */

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


public class ReadCAS {

    private final String osName = System.getProperty("os.name");
    private  List<String> casList = new ArrayList<>();

    public ReadCAS() throws ParseException {


        boolean isWinDev = false;
        if(osName != null && osName.toLowerCase().contains("windows") )   isWinDev = true;

        File dir = null;
        if(isWinDev) {

            dir = new File("C:\\mnt\\pdata\\cas.txt");
        } else{

            dir = new File("/mnt/pdata/cas.txt");
        }

        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new FileReader(dir));
            String line;
            while(  (line = reader.readLine())  != null  ) {

                String cas = line.toLowerCase().trim();
                casList.add(cas);
            }



        } catch (IOException e) {

            System.out.println("Could not read cas.txt");
        } finally {

            if(reader != null) {


                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }




    }

    public List<String> getCasList() {

        return casList;
    }


}
