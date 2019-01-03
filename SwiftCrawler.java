import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A library for finding all links and broken links on a webpage.
 * @author Harsh Baheti
 * 3rd January 2019
 */
public class SwiftCrawler
{
    /**
     * Creates a connection to the specified URL
     * @param root URL to connect to
     * @return reference to connection object
     */
    public URLConnection getConnection(String root)
    {
        URL url;
        URLConnection connection;

        if(root == null || root.length()==0)
        {
            throw new RuntimeException("Invalid URL Link");
        }

        try
        {
            url = new URL(root);
            connection = url.openConnection();
            return connection;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException("Connection could not be established");
        }
    }

    /**
     * Fetch the links till the desired level of depth
     * @param root rool URL to start search from
     * @param level level of depth
     * @return all links identified
     * @throws IOException exception
     */
    public HashSet<String> getLinks(String root, int level) throws IOException
    {
        HashSet<String> allLinks = new HashSet<>();
        HashSet<String> results = getLinks(root);

        if(level <= 0)
        {
            return null;
        }
        else if(level==1)
        {
            return results;
        }
        else
        {
            HashSet<String> subResults;

            while (level>1)
            {
                for(String link : results)
                {
                    if(!allLinks.contains(link))
                    {
                        subResults = getLinks(link);
                        if(subResults!=null)
                        {
                            for (String subLink : subResults)
                            {
                                allLinks.add(subLink);
                            }
                        }
                    }
                }
                level--;
            }
        }

        for(String s : results)
        {
            allLinks.add(s);
        }

        return allLinks;
    }

    /**
     * Returns the list of links present on the web page given as argument to this method.
     * Returns null if the URL is malformed or if the link is unreachable.
     * Returns an empty list if there are no links for the web page.
     * @param root link of web page to search on
     * @return list of links | null if URL is malformed/unreachable | empty list if no links are present
     */
    public HashSet<String> getLinks(String root) throws IOException
    {
        HashSet<String> links = new HashSet<>();
        URLConnection connection = getConnection(root);
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer sb = new StringBuffer();

        while ((inputLine = in.readLine()) != null)
        {
            sb = sb.append(inputLine+"\n");
        }
        in.close();

        Document document = Jsoup.parse(sb.toString());
        Elements anchors = document.body().getElementsByAttribute("href");

        for(Element e : anchors)
        {
            String href = e.attr("href");
            if(!(href.startsWith("#") || href.startsWith("tel") || href.startsWith("?") || href.startsWith("mailto:") || href.startsWith("index")))
            {
                if(!(href.startsWith("http") || href.startsWith("www.")))
                {
                    if(href.startsWith("../"))
                    {
                        String tempURL = "";
                        while(href.startsWith("../"))
                        {
                            if(tempURL.length()==0)
                                tempURL = connection.getURL().toString().substring(0,connection.getURL().toString().length()-1);
                            tempURL = tempURL.substring(0,tempURL.lastIndexOf("/"));
                            href = href.substring(3);
                            int k=0;
                        }
                        if(tempURL.length()<(connection.getURL().getProtocol()+"://"+connection.getURL().getHost()).length())
                        {
                            links.add(connection.getURL().getProtocol()+"://"+connection.getURL().getHost()+"/"+href);
                        }
                        else
                        {
                            links.add(tempURL+"/"+href);
                        }
                    }
                    else if(href.startsWith("/"))
                    {
                        links.add(connection.getURL().getProtocol()+"://"+connection.getURL().getHost()+href);
                    }
                    else
                    {
                        links.add(connection.getURL()+href);
                    }
                }
                else
                {
                    links.add(href);
                }
            }
        }
        return links;
    }

    /**
     * Returns all the broken links (links which returned server response 404) and the response code.
     * @param root link of webpage to search broken links on
     * @return broken link and response code (response code value will be -1 if connection was not successful)
     */
    public HashMap<String,Integer> findBrokenLinks(String root)
    {
        try
        {
            HashSet<String> links = getLinks(root);
            return findBrokenLinks(links);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns all the broken links (links qhich returned server response code other than 200) and the response code.
     * @param links list of links to check for breakage
     * @return broken link and response code (response code value will be -1 if connection was not successful)
     * @throws ProtocolException Exception thrown if method name was incorrect
     */
    public HashMap<String,Integer> findBrokenLinks(HashSet<String> links) throws ProtocolException
    {
        int responseCode = -1;
        HashMap<String,Integer> brokenLinks = new HashMap<>();

        for(String link : links)
        {
            URLConnection con;
            con = getConnection(link);

            try
            {
                if(con instanceof HttpsURLConnection)
                {
                    HttpsURLConnection httpsCon = (HttpsURLConnection) con;
                    httpsCon.setRequestMethod("HEAD");
                    responseCode = httpsCon.getResponseCode();
                }
                else if (con instanceof HttpURLConnection)
                {
                    HttpURLConnection httpCon = (HttpURLConnection) con;
                    httpCon.setRequestMethod("HEAD");
                    responseCode = httpCon.getResponseCode();
                }

                if(responseCode==404)
                {
                    brokenLinks.put(link,responseCode);
                }
            }
            catch (IOException e)
            {
                brokenLinks.put(link,-1);
            }
        }
        return brokenLinks;
    }
}
