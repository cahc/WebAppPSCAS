package se.cc.pscas;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Created by crco0001 on 2/2/2018.
 */
import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import org.jasypt.util.password.BasicPasswordEncryptor;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@WebServlet(name = "MasterServlet", urlPatterns = {"/login","/secured","/logout","/search","/checkStatus","/addCas"}, loadOnStartup = 1,

        initParams =  {@WebInitParam(name = "Admin",value="Apan Ola"), @WebInitParam(name = "Admin2",value="Apan Ola2") }

)


public class MasterServlet extends HttpServlet {

    private final String osName = System.getProperty("os.name");
    private static final long serialVersionUID = 1L;
    private ScheduledExecutorService backgroundExecutor;
    private final AtomicReference<List<Person>> cachedPersons = new AtomicReference<List<Person>>();
    private final AtomicReference<String> updated = new AtomicReference<String>();
    private final HashSet<String> authoritizedCAS = new HashSet<>();



    /**
     * @see HttpServlet#HttpServlet()
     */
    public MasterServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**

     */
    public void init(ServletConfig config) throws ServletException {
        //initialize to empty
        updated.set("null");
        List<Person> emptyList = new ArrayList<>();
        cachedPersons.set(emptyList);

        backgroundExecutor = Executors.newSingleThreadScheduledExecutor();
        backgroundExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

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

            }
        }, 0, 12, TimeUnit.HOURS);


        try {
            ReadCAS cas = new ReadCAS();

            this.authoritizedCAS.addAll(cas.getCasList());

        } catch (ParseException e) {

            System.out.println(e.getLocalizedMessage());
        }

    }


    public static List<String> getHTML(String urlToRead) throws Exception {
        List<String> result = new ArrayList<>(2);
        URL url = new URL(urlToRead);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {

            result.add(line);
        }
        rd.close();
        return result;
    }

    private synchronized boolean addTrustedCas(String cas) {
        boolean itWorked = true;
        boolean isWinDev = false;
        boolean isMacDev = false;

        if(osName != null && osName.toLowerCase().contains("windows") )   isWinDev = true;
        if(osName != null && osName.toLowerCase().contains("mac os") )   isMacDev = true;

        BufferedWriter writer = null;
        File file = null;
        if(isWinDev) {

            file = new File("C:\\mnt\\pdata\\cas.txt");
        } else if(isMacDev){

            file = new File("/Users/cristian/pdata/cas.txt");
        } else {

            file = new File("/mnt/pdata/cas.txt");
        }


        try {
            writer = new BufferedWriter(new FileWriter(file,true) );
            writer.newLine();
            writer.append(cas);
            writer.newLine();
        } catch (IOException e) {

            System.out.println(e.getLocalizedMessage());
            itWorked = false;

        } finally {

            if(writer != null) {

                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {

                    System.out.println("Could not close cas.txt");


                }


            }

            return itWorked;
        }

    }


    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("utf-8");

        String ticket = request.getParameter("ticket");

        if (ticket == null) {

            //redirect to CAS server
            response.sendRedirect("https://cas.umu.se/index.jsp?service=https://delta.ub.umu.se/login");


        } else {

            //check if the login was valid, and if so get the user
            String url = "https://cas.umu.se/validate?ticket=" + ticket + "&service=https://delta.ub.umu.se/login";

            List<String> validationBody = null;
            String user = null;

            boolean success = false;
            try {
                validationBody = getHTML(url);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //todo remove debug
            System.out.println((validationBody == null) ? "validationBodyisNull" : validationBody.toString());

            if (validationBody != null) {

                //success = validationBody.contains("<cas:authenticationSuccess>");
                success = validationBody.get(0).contains("yes");
                if (success) user = validationBody.get(1);

                if (user == null) success = false;

            }

            if (!success) {
                Cookie cookie = new Cookie("ErrorType", "validationCallFailed"); //for debugging //todo remove
                cookie.setMaxAge(-1);
                response.addCookie(cookie);
                request.getRequestDispatcher("WEB-INF/error.html").forward(request, response);

            } else {


                //finally check if it is an allowed cas/user

                boolean specialPrivilige = this.authoritizedCAS.contains(user.toLowerCase());

                if(specialPrivilige) {

                    //create a session
                    HttpSession session = request.getSession(true);
                    session.setAttribute("userAllowed", true);

                    request.getRequestDispatcher("/secured").forward(request, response);

                } else {


                    request.getRequestDispatcher("error.html").forward(request, response);

                }


            }


        }


    }

    private void handleSecured(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("utf-8");
        request.setCharacterEncoding("utf-8");
        //check session object, forward to error page with error code.. TODO
        HttpSession session = request.getSession(false);

        if (session == null) {
            request.getRequestDispatcher("login").forward(request, response);

            //PrintWriter out = response.getWriter();
            //out.println("no session!");
            return;
        }
        Object attribut = session.getAttribute("userAllowed");
        if (attribut == null) {
            request.getRequestDispatcher("login").forward(request, response);
            // PrintWriter out = response.getWriter();
            // out.println("no userAllowed attribut exist ");
            return;
        }
        if (!((Boolean) attribut)) {
            request.getRequestDispatcher("login").forward(request, response);
            //PrintWriter out = response.getWriter();
            //out.println("userAllowed==FALSE");
            return;
        }

        //forward to search page if no search parameters are given


        request.getRequestDispatcher("WEB-INF/index.html").forward(request, response);

    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("utf-8");

        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        //PrintWriter out = response.getWriter();
        //out.println("Session now invalidated..");
        response.sendRedirect("https://cas.umu.se/logout");


    }

    private void handleSearch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        /*
        ////not allowed to access this without being logged in//////////////////

        HttpSession session = request.getSession(false);

        if (session == null) {
            request.getRequestDispatcher("login").forward(request, response);

            //PrintWriter out = response.getWriter();
            //out.println("no session!");
            return;
        }
        Object attribut = session.getAttribute("userAllowed");
        if (attribut == null) {
            request.getRequestDispatcher("login").forward(request, response);
            // PrintWriter out = response.getWriter();
            // out.println("no userAllowed attribut exist ");
            return;
        }
        if (!((Boolean) attribut)) {
            request.getRequestDispatcher("login").forward(request, response);
            //PrintWriter out = response.getWriter();
            //out.println("userAllowed==FALSE");
            return;
        }

        /////////////////////////////////////////////////////////////////////////

*/


        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("utf-8");

        System.out.println("cashed person object is null? " + (this.cachedPersons == null)); //debug print
        System.out.println("request: " + request.getQueryString()); //debug print
        String status = request.getParameter("serverstatus");

        if (status != null) {
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            //  String resp = "<span class=\"badge\"><div id=\"nrobj\">" +this.cachedPersons.get().size() + "</div></span> personobjekt uppdaterade <span class=\"badge\"><div id=\"updated\">" + this.updated.get() + "</div></span>";
            // String resp = "  <span class=\"badge\"><div id=\"nrobj\">" +this.cachedPersons.get().size() + "</div></span> personobjekt uppdaterade <span class=\"badge\"><div id=\"updated\">" + this.updated.get() + "</div></span>";

            String resp = "Databas uppdaterad <span class=\"badge\"><div id=\"updated\">" + this.updated.get() + "</div></span>" + ". Innehåller " + "<span class=\"badge\"><div id=\"nrobj\">" + this.cachedPersons.get().size() + "</div></span>" + " personobjekt.";
            out.print(resp);
            out.flush();
            out.close();
            return;
        }

        String name = request.getParameter("name");
        String cas = request.getParameter("cas");
        String algo = request.getParameter("algo");
        String maxhits = request.getParameter("maxhits");

        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        if (name.length() == 0 && cas.length() == 0) {

            out.println("<font color=\"red\"><strong>Ogiltig sökfråga!<strong></font>");
            out.flush();
            out.close();
        } else if (cas.length() != 0) {

            //use exact CAS search
            ArrayList<Person> casList = new ArrayList<>(5);
            cas = cas.toLowerCase().trim();

            for (int i = 0; i < cachedPersons.get().size(); i++) {

                Person p = cachedPersons.get().get(i);

                if (cas.equals(p.getUID())) {

                    casList.add(p);

                }

            }
            out.println(PrintResult.printCas(casList));
            out.flush();
            out.close();
        } else {

            //USE FUZZY SEARCH OR EXAKT..

            Map<String, Integer> nameQueryProfile = Person.ks.getProfile(Person.normalizeText(name));

            String normalizedNameQuery = Person.normalizeText(name);
            String[] initialEfternamn = normalizedNameQuery.split(" ");


            int topN = Integer.valueOf(maxhits);

            Collector<Person, Double> personCollector = new Collector<Person, Double>(topN); //  top 10 hits


            for (int i = 0; i < cachedPersons.get().size(); i++) {

                Person p = cachedPersons.get().get(i);
                Double sim = -1.0;
                if ("N-gram".equals(algo)) try {


                    Cosine cosine = new Cosine();

                    sim = cosine.similarity(nameQueryProfile, p.stringProfile);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                if ("Levenshtein".equals(algo)) {
                    NormalizedLevenshtein levenshtein = new NormalizedLevenshtein();

                    sim = levenshtein.similarity(normalizedNameQuery, p.NormalizedDisplayName);
                }

                if ("exakt".equals(algo)) {

                    if (initialEfternamn.length != 2) {

                        out.println("<font color=\"red\"><strong>Ogiltig sökfråga!<strong></font>");
                        out.flush();
                        out.close();
                        return;
                    }


                    if (p.getNormalizedGivenName().startsWith(initialEfternamn[0]) && p.getNormalizedSurName().startsWith(initialEfternamn[1])) {

                        sim = 1.0;
                    }

                }


                if (sim >= 0.4) { // not meaningful to show hits with lower similarity

                    personCollector.offer(p, sim);

                }


            }


            out.println(PrintResult.printCollector(personCollector));
            out.flush();
            out.close();

        }




    }

    private void checkStatus(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        int n = this.authoritizedCAS.size();


        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("utf-8");

        response.getWriter().println(n + "CAS LOADED!");


    }

    private void handleAddCas(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {


        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.setCharacterEncoding("utf-8");

        String castoadd = request.getParameter("add_cas");
        String plainTextPassword = request.getParameter("password");


        BasicPasswordEncryptor textEncryptor = new BasicPasswordEncryptor();
        String encryptedPassword = "M7yEv5WaVQDBppmuPJfkQ0ibN078a9TL";

        PrintWriter writer = response.getWriter();

        if (textEncryptor.checkPassword(plainTextPassword, encryptedPassword)) {

            if(castoadd == null || castoadd.length() < 4) {

                writer.println("trying to add null och invalid cas!");

            } else {

                //persist to file
                boolean check = addTrustedCas( castoadd.trim().toLowerCase()  );

                //had to hashTable
                this.authoritizedCAS.add(castoadd.trim().toLowerCase() );


                writer.println("add operation successful: " + check);

            }


        } else {


            writer.println("wrong password!");

        }


    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String uri = request.getRequestURI();

        System.out.println(uri);

        if (uri.endsWith("/secured")) {

            handleSecured(request, response);

        } else if (uri.endsWith("/logout")) {

            handleLogout(request, response);

        } else if (uri.endsWith("/login")) {

            handleLogin(request, response);

        } else if (uri.endsWith("/search")) {

            handleSearch(request,response);

        } else if(uri.endsWith("/checkStatus")) {

            checkStatus(request,response);
        } else if(uri.endsWith("/addCas")) {

            handleAddCas(request,response);
        }


    }


}
