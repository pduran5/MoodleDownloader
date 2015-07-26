package moodledownloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MoodleDownloader {
    
    private int idx, nlinks;
    private String folder, moodleURL;
    private Map<String, String> cookies;
    
    private static MainJFrame mainframe;

    public static void main(String[] args) {
        mainframe = new MainJFrame();
        mainframe.setVisible(true);
    }

    public void downloadMoodle(String url) {
        moodleURL = url;
        Document doc = moodleLogin();
        countLinks(doc);
        parseLinks(doc);
        mainframe.showCompleted();
    }

    private void countLinks(Document doc) {
        Elements links = doc.select("a[href]");
        nlinks=0;
        for (Element e : links) {
            if (e.toString().contains("/assign/") || e.toString().contains("/folder/")
                    || e.toString().contains("/forum/") || e.toString().contains("/page/")
                    || e.toString().contains("/resource/") || e.toString().contains("/url/")) {
                nlinks++;
            }
        }
        mainframe.setMaximumProgressBar(nlinks);
    }

    private Document moodleLogin() {
        trustEveryone();
        
        Document doc = null;
        
        try {
            Response response = Jsoup.connect(moodleURL).method(Method.GET).maxBodySize(0).timeout(0).execute();
            doc = response.parse();
            cookies = response.cookies();

            if(doc.toString().contains("loginform")) {
                String loginURL = doc.baseUri();

                response = Jsoup.connect(loginURL).method(Method.GET).maxBodySize(0).timeout(0).execute();
                doc = response.parse();
                cookies = response.cookies();

                response = Jsoup.connect(loginURL)
                        .data("username", mainframe.getUsername())
                        .data("password", mainframe.getPassword())
                        .cookies(cookies).maxBodySize(0).timeout(0).method(Method.POST).execute();
                doc = response.parse();

                Map <String, String> tmpcookies; 
                tmpcookies = response.cookies();

                String cookieuser=null, cookiesession = null;

                for (Map.Entry<String, String> entrySet : tmpcookies.entrySet()) {
                    String key = entrySet.getKey();
                    String value = entrySet.getValue();
                    if (key.contains("MoodleSession")) {
                        cookieuser = key;
                        cookiesession = value;
                    }
                }

                cookies.put(cookieuser, cookiesession);

                response = Jsoup.connect(moodleURL).method(Method.GET).cookies(cookies).maxBodySize(0).timeout(0).execute();
                doc = response.parse();
            }

            folder = mainframe.getFolder();
        } catch (IOException ioe) {
            System.out.println("ERROR: " + ioe.toString());
        }

        return doc;
    }

    private void parseLinks(Document doc) {
        Elements links = doc.select("a[href]");
        String name, link, type=null;
        idx=1;

        for (Element e : links) {
            link = e.attr("href");
            name = e.text().replaceAll("[\\\\/:*?\"<>|]", "-");
            String content = e.toString();
            
            if (content.contains("/assign/")) {
                if(name.length() > 6)
                    if(name.substring(name.length()-6).equals(" Tasca"))
                        name = name.substring(0, name.length()-6);
                downloadAssignment(name, link);
                idx++;
            }
            if (content.contains("/folder/")) {
                if(name.length() > 8)
                    if(name.substring(name.length()-8).equals(" Carpeta"))
                        name = name.substring(0, name.length()-8);
                downloadFolder(name, link);
                idx++;
            }
            if (content.contains("/forum/")) {
                if(name.length() > 6)
                    if(name.substring(name.length()-6).equals(" Fòrum"))
                        name = name.substring(0, name.length()-6);
                downloadForum(name, link);
                idx++;
            }
            if (content.contains("/page/")) {
                if(name.length() > 7)
                    if(name.substring(name.length()-7).equals(" Pàgina"))
                        name = name.substring(0, name.length()-7);
                downloadPage(name, link);
                idx++;

            }
            if (content.contains("/resource/")) {
                if(name.length() > 7)
                    if(name.substring(name.length()-7).equals(" Fitxer"))
                        name = name.substring(0, name.length()-7);
                
                if(content.contains("archive-")) type=".zip";
                if(content.contains("avi-")) type=".avi";
                if(content.contains("bmp-")) type=".bmp";
                if(content.contains("document-")) type=".doc";
                if(content.contains("flash-")) type=".swf";
                if(content.contains("gif-")) type=".gif";
                if(content.contains("jpeg-")) type=".jpg";
                if(content.contains("mp3-")) type=".mp3";
                if(content.contains("mpeg-")) type=".mpg";
                if(content.contains("powerpoint-")) type=".ppt";
                if(content.contains("png-")) type=".png";
                if(content.contains("pdf-")) type=".pdf";
                if(content.contains("quicktime-")) type=".mov";
                if(content.contains("spreadsheet-")) type=".xls";
                if(content.contains("sourcecode-")) type=".txt";
                if(content.contains("text-")) type=".txt";
                if(content.contains("unknown-")) type="";
                if(content.contains("writer-")) type=".odt";
                
                downloadResource(name, link, type);
                idx++;
            }
            if (content.contains("/url/")) {
                if(name.length() > 4)
                    if(name.substring(name.length()-4).equals(" URL"))
                        name = name.substring(0, name.length()-4);
                downloadURL(name, link);
                idx++;
            }
        }
    }

    private void trustEveryone() {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        };
        
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        }
        catch (NoSuchAlgorithmException | KeyManagementException e) {}
    }
    
    private void downloadFolder(String name, String link) {
        mainframe.setProgressBar(idx);
        mainframe.setOut("["+idx+"/"+nlinks+"] [Carpeta] " + name);
        
        try {
            Document carpeta = Jsoup.connect(link).cookies(cookies).maxBodySize(0).timeout(0).get();
            Elements links = carpeta.select("a[href]");

            for (Element e : links) {
                if (e.toString().contains("/pluginfile.php/")) {
                    String lnk = e.attr("href");
                    String nam = null;

                    if(e.textNodes().size() > 0) {
                        nam = String.format("%03d", idx) + " [Fitxer] " + e.textNodes().get(0);
                        mainframe.setOut("["+idx+"/"+nlinks+"] [Fitxer] " + e.textNodes().get(0));
                    }

                    downloadFile(nam, lnk);
                }
            }
        } catch (IOException ioe) {
            System.out.println("ERROR: " + ioe.toString());
        }   
    }

    private void downloadURL(String name, String link) {
        name = name.replaceAll("[\\\\/:*¿?\"<>|]", "-");
        name = name.replaceAll("[à]", "a");
        name = name.replaceAll("[é]", "e");
        name = name.replaceAll("[í]", "i");
        name = name.replaceAll("[ó]", "o");
        name = name.replaceAll("[ç]", "c");

        mainframe.setProgressBar(idx);
        mainframe.setOut("["+idx+"/"+nlinks+"] [URL] " + name);

        try {
            Document urlcontent = Jsoup.connect(link).cookies(cookies).timeout(0).maxBodySize(0).get();

            PrintWriter fitxer = new PrintWriter(new FileWriter(folder + "/" + String.format("%03d", idx) + " [URL] " + name.replaceAll("[()']", "") + ".url"));
            Elements content = urlcontent.select("div.urlworkaround > a");
            // TODO: Revisar select de frame, ya que falla si abre nueva web con frames
            Elements frame = urlcontent.select("frame");

            if(content.size()>0) {
                fitxer.println("[InternetShortcut]");
                fitxer.println("URL="+content.attr("href"));
            } else if(frame.size()>0) {
                String resourcelink;
                for (Element e : frame) {
                    resourcelink = e.attr("src");
                    if(!resourcelink.contains("frameset")) {
                        fitxer.println("[InternetShortcut]");
                        fitxer.println("URL="+resourcelink);
                    }
                }
            } else {
                fitxer.println("[InternetShortcut]");
                fitxer.println("URL="+urlcontent.location());
            }

            fitxer.close();
        } catch (IOException ioe) {
            mainframe.setOut("ERROR: " + ioe.toString());
        }
    }

    private void downloadPage(String name, String link) {
        mainframe.setProgressBar(idx);
        mainframe.setOut("["+idx+"/"+nlinks+"] [Pàgina] " + name);
        
        try {
            Document forum = Jsoup.connect(link).cookies(cookies).maxBodySize(0).timeout(0).get();
            Elements content = forum.select("div.box > div.no-overflow");

            BufferedWriter fileforum = new BufferedWriter(new FileWriter(folder + "/" + String.format("%03d", idx) + " [Pàgina] " + name + ".html"));
            fileforum.write("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"+content.html()+"</html>");
            fileforum.close();
        } catch (IOException ioe) {
            System.out.println("ERROR: " + ioe.toString());
        }
    }

    private void downloadAssignment(String name, String link) {
        mainframe.setProgressBar(idx);
        mainframe.setOut("["+idx+"/"+nlinks+"] [Tasca] " + name);
        
        Document forum = null;
        
        try {
            forum = Jsoup.connect(link).cookies(cookies).maxBodySize(0).timeout(0).get();
            Elements content = forum.select("div.box > div.no-overflow");

            BufferedWriter fileforum = new BufferedWriter(new FileWriter(folder + "/" + String.format("%03d", idx) + " [Tasca] " + name + ".html"));
            fileforum.write("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"+content.html()+"</html>");
            fileforum.close();
        } catch (IOException ioe) {
            System.out.println("ERROR: " + ioe.toString());
        }
        
        Elements resources = forum.select("div.box > div.no-overflow > a");
        String resourcelink = resources.attr("href");
        if(resourcelink.contains(".pdf")) {
            mainframe.setOut("["+idx+"/"+nlinks+"] [Fitxer] " + name);
            downloadFile(name, resourcelink);
        }
    }

    private void downloadForum(String name, String link) {
        mainframe.setProgressBar(idx);
        mainframe.setOut("["+idx+"/"+nlinks+"] [Fòrum] " + name);
        
        try {
            Document forum = Jsoup.connect(link).cookies(cookies).maxBodySize(0).timeout(0).get();
            Elements content = forum.select("div.box > div.no-overflow");

            BufferedWriter fileforum = new BufferedWriter(new FileWriter(folder + "/" + String.format("%03d", idx) + " [Fòrum] " + name + ".html"));
            fileforum.write("<html><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"+content.html()+"</html>");
            fileforum.close();
        } catch (IOException ioe) {
            System.out.println("ERROR: " + ioe.toString());
        }
    }

    private void downloadResource(String name, String link, String type) {
        String iname = String.format("%03d", idx) + " [Fitxer] " + name + type;

        mainframe.setProgressBar(idx);
        mainframe.setOut("["+idx+"/"+nlinks+"] [Fitxer] " + name + type);
        
        downloadFile(iname, link);
    }
    
    private void downloadFile(String name, String link) {
        try {
            Response resultImageResponse = Jsoup.connect(link).cookies(cookies).ignoreContentType(true).maxBodySize(0).timeout(0).execute();   
            Document doc = resultImageResponse.parse();

            if(doc.html().contains("resourcecontent resourcepdf"))
                downloadFile(name, doc.select("object").attr("data"));
            else if(doc.html().contains("resourceworkaround"))
                downloadFile(name, doc.select(".resourceworkaround > a").attr("href"));
            else {
                FileOutputStream out = new FileOutputStream(new File(folder, name));
                out.write(resultImageResponse.bodyAsBytes());
            }
        } catch (IOException ioe) {
            System.out.println("ERROR: " + ioe.toString());
        }    
    }

}
