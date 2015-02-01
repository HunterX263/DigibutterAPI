package uk.org.g33k.digibutterapi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import uk.org.g33k.digibutterapi.betalands.Betalands;

public class Digibutter {
	private String username;
	private String password;
	private int userId;
	private Map<String, String> cookies = new HashMap<String, String>();
	private boolean debug = false;
	private SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	private WebSocketClient betalandsClient;
	
	/**
	 * Create a new Digibutter object.
	 * @param username The username to use when logging in.
	 * @param password The password to use when logging in.
	 */
	public Digibutter(String username, String password)
	{
		this.username = username;
		this.password = password;
		HttpURLConnection.setFollowRedirects(false);
	}
	
	/**
	 * Create a new Digibutter object.
	 * @param username The username to use when logging in.
	 * @param password The password to use when logging in.
	 * @param debug Set to true to enable debug message logging.
	 */
	public Digibutter(String username, String password, boolean debug)
	{
		this.username = username;
		this.password = password;
		this.debug = debug;
		HttpURLConnection.setFollowRedirects(false);
	}
	
	/**
	 * Log in to the website.
	 * @throws IOException
	 */
	public void login() throws IOException
	{
		log("Logging in...");
		URL url = new URL("http://digibutter.nerr.biz/auth/basic");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		String content = "username=" + username + "&password=" + password;
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		connection.setRequestProperty("Content-Length", Integer.toString(content.getBytes().length));
		connection.setDoOutput(true);
		
		DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
		stream.writeBytes(content);
		stream.close();
				
		updateCookies(connection);
		connection.disconnect();
		
		userId = getUserId();
		
		log("Done.");
	}
	
	/**
	 * Log out of the website.
	 * @throws IOException
	 */
	public void logout() throws IOException
	{
		log("Logging out...");
		URL url = new URL("http://digibutter.nerr.biz/auth/logout");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		connection.setRequestProperty("Cookie", cookieString());
			
		connection.connect();
		updateCookies(connection);
		connection.disconnect();
		
		log("Done.");
	}
	
	/**
	 * Posts a new topic to the logged-in user's board.
	 * @param title The title of the topic.
	 * @param message The extended body of the topic.
	 * @return The ID number of the newly created topic.
	 * @throws IOException
	 */
	public int postTopic(String title, String message) throws IOException
	{
		ensureLogin();
		
		log("Posting topic...");
		URL url = new URL("http://digibutter.nerr.biz/topics/submit");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		String content = "title=" + title + "&urls[]=&boardidselect=" + username + "&boardid=&boardiddefault=" + userId + "&body=&ckBody=" + message;
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		connection.setRequestProperty("Cookie", cookieString());
		connection.setRequestProperty("Content-Length", Integer.toString(content.getBytes().length));
		connection.setDoOutput(true);
		
		DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
		stream.writeBytes(content);
		stream.close();
			
		InputStream input = connection.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String response = reader.readLine();
		Document doc = Jsoup.parse(response);
		String[] split = doc.getElementsByTag("a").get(0).attr("href").split("/");
		String topicId = split[2];
			
		updateCookies(connection);
		connection.disconnect();
		
		log("Done. (" + topicId + ")");
		return Integer.parseInt(topicId);
	}
	
	/**
	 * Posts a reply to the specified topic.
	 * @param topicId The ID of the topic to reply to.
	 * @param value The content of the reply.
	 * @throws IOException
	 */
	public void postReply(String topicId, String value) throws IOException
	{
		ensureLogin();
		
		log("Posting reply...");
		URL url = new URL("http://digibutter.nerr.biz/comments/submit");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		String content = "value=" + value + "&topicid=" + topicId;
		log(content);
		//content = content.replace(' ', '+');
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		connection.setRequestProperty("Cookie", cookieString());
		connection.setRequestProperty("Content-Length", Integer.toString(content.getBytes().length));
		connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		connection.setDoOutput(true);
		
		DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
		stream.writeBytes(content);
		stream.close();
		
		updateCookies(connection);
		connection.disconnect();
		
		log("Done.");
	}
	
	/**
	 * Gets the topics on the first page of the site.
	 * @return A list of topics.
	 * @throws IOException
	 * @throws ParseException
	 */
	public ArrayList<Topic> getTopics() throws IOException, ParseException
	{
		return getTopics(0);
	}
	
	/**
	 * Gets the topics on the specified page.
	 * @param page The page to get topics from.
	 * @return A list of topics.
	 * @throws IOException
	 * @throws ParseException
	 */
	public ArrayList<Topic> getTopics(int page) throws IOException, ParseException
	{
		log("Getting topics on page " + page + "...");
		ArrayList<Topic> output = new ArrayList<Topic>();
		URL url = new URL("http://digibutter.nerr.biz/topics");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		while (page > 0)
		{
			Document doc = Jsoup.parse(html);
			Elements topics = doc.getElementById("topic-container").getElementsByClass("listtopic");
			Element nextPage = topics.get(topics.size() - 1).getElementsByTag("a").get(0);
			
			url = new URL("http://digibutter.nerr.biz" + nextPage.attr("href"));
			connection = (HttpURLConnection)url.openConnection();
			
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Cookie", cookieString());
			
			connection.connect();
			
			html = getHtml(connection);
			updateCookies(connection);
			connection.disconnect();
			
			page--;
		}
		
		Document doc = Jsoup.parse(html);
		Elements topics = doc.getElementById("topic-container").getElementsByClass("listtopic");
		
		for (int loop = 0; loop < topics.size() - 1; loop++)
		{
			Element topic = topics.get(loop);
			Topic newTopic = new Topic();
			
			newTopic.id = Integer.parseInt(topic.id().replaceAll("topic-", ""));
			newTopic.username = topic.getElementsByClass("topicinfo").get(0).getElementsByClass("date").get(0).getElementsByTag("a").get(0).getElementsByTag("span").get(0).ownText();
			newTopic.title = topic.getElementsByClass("title").get(0).getElementsByTag("a").get(0).ownText();
			String date = topic.getElementsByClass("topicinfo").get(0).getElementsByClass("date").get(0).getElementsByTag("abbr").get(0).attr("title");
			date = date.replaceAll(":(\\d\\d)$", "$1");
			newTopic.date = dateParser.parse(date);
			newTopic.message = topic.getElementsByClass("topicarea").get(0).getElementsByClass("body").get(0).html();
			
			output.add(newTopic);
		}
		
		log("Done.");
		return output;
	}
	
	/**
	 * Gets a specified topic.
	 * @param topicId The ID of the topic to get.
	 * @return The specified topic.
	 * @throws IOException
	 * @throws ParseException
	 */
	public Topic getTopic(int topicId) throws IOException, ParseException
	{
		log("Getting topic with id " + topicId + "...");
		URL url = new URL("http://digibutter.nerr.biz/topics/" + topicId);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Topic output = new Topic();
		Document doc = Jsoup.parse(html);
		output.id = topicId;
		output.title = doc.getElementById("topic").getElementsByClass("title").get(0).ownText();
		output.username = doc.getElementById("topic").getElementsByClass("date").get(0).getElementsByTag("a").get(0).getElementsByTag("span").get(0).ownText();
		String date = doc.getElementById("topic").getElementsByClass("date").get(0).getElementsByTag("abbr").get(0).attr("title");
		date = date.replaceAll(":(\\d\\d)$", "$1");
		output.date = dateParser.parse(date);
		output.message = doc.getElementById("topic").getElementsByClass("topicarea").get(0).getElementsByClass("body").get(0).html();
		
		log("Done.");
		return output;
	}
	
	/**
	 * Gets the replies to a specified topic.
	 * @param topicId The ID of the topic to check.
	 * @return A list of replies.
	 * @throws IOException
	 * @throws ParseException
	 */
	public ArrayList<Reply> getReplies(int topicId) throws IOException, ParseException
	{
		log("Getting replies...");
		ArrayList<Reply> output = new ArrayList<Reply>();
		URL url = new URL("http://digibutter.nerr.biz/topics/" + topicId);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		
		Document doc = Jsoup.parse(html);
		Elements replies = doc.getElementById("commentarea").getElementsByClass("commentrow");
		for (Element reply : replies)
		{
			Reply r = new Reply();
			r.username = reply.getElementsByClass("date").get(0).getElementsByTag("span").get(0).getElementsByTag("a").get(0).ownText();
			String date = reply.getElementsByClass("date").get(0).getElementsByTag("span").get(0).getElementsByTag("abbr").get(0).attr("title");
			date = date.replaceAll(":(\\d\\d)$", "$1");
			r.date = dateParser.parse(date);
			reply.getElementsByClass("commentavatar-view").remove();
			reply.getElementsByClass("date").remove();
			r.message = reply.html();
			output.add(r);
		}
		
		updateCookies(connection);
		connection.disconnect();
		
		log("Done.");
		return output;
	}
	
	/**
	 * Gets the display name of the logged-in user.
	 * @return The user's name.
	 * @throws IOException
	 */
	public String getDisplayName() throws IOException
	{
		ensureLogin();
		
		log("Getting display name...");
		String output = "";
		URL url = new URL("http://digibutter.nerr.biz/");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Document doc = Jsoup.parse(html);
		Element cbox = doc.getElementById("cboxmain");
		String src = cbox.attr("src");
		Pattern pattern = Pattern.compile("(?<=usr\\=)[\\w\\s]+");
		Matcher matcher = pattern.matcher(src);
		if (matcher.find())
		{
		    output = matcher.group();
		}
		
		log("Done.");
		return output;
	}
	
	/**
	 * Get the ID of the logged-in user.
	 * @return The ID of the user, or -1 if there was an issue.
	 * @throws IOException
	 */
	public int getUserId() throws IOException
	{
		ensureLogin();
		
		log("Getting user ID..");
		URL url = new URL("http://digibutter.nerr.biz/profile/" + username);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Document doc = Jsoup.parse(html);
		Element tabs = doc.getElementById("tabnav");
		String board = tabs.getElementsByClass("tab2").get(0).getElementsByTag("a").get(0).attr("href");
		
		Pattern pattern = Pattern.compile("(?<=\\/board\\/)\\d+");
		Matcher matcher = pattern.matcher(board);
		if (matcher.find())
		{
		    return Integer.parseInt(matcher.group());
		}
		else
		{
			return -1;
		}
	}
	
	/**
	 * Gets the name of the logged-in user's avatar.
	 * @return The user's avatar ID.
	 * @throws IOException
	 */
	public String getAvatarId() throws IOException
	{
		ensureLogin();
		
		log("Getting avatar ID...");
		String output = "";
		URL url = new URL("http://digibutter.nerr.biz/");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Document doc = Jsoup.parse(html);
		Element cbox = doc.getElementById("cboxmain");
		String src = cbox.attr("src");
		log(src);
		Pattern pattern = Pattern.compile("(?<=avatar\\=)\\w+");
		Matcher matcher = pattern.matcher(src);
		if (matcher.find())
		{
		    output = matcher.group();
		}
		
		log("Done.");
		return output;
	}
	
	/**
	 * Gets the authentication code for the chat box for the logged-in user.
	 * @return The chat authentication code.
	 * @throws IOException
	 */
	private String getChatAuth() throws IOException
	{
		ensureLogin();
		
		log("Getting Chatbox auth key...");
		String output = "";
		URL url = new URL("http://digibutter.nerr.biz/");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Document doc = Jsoup.parse(html);
		Element cbox = doc.getElementById("cboxmain");
		String src = cbox.attr("src");
		log(src);
		Pattern pattern = Pattern.compile("(?<=h\\=)\\w+");
		Matcher matcher = pattern.matcher(src);
		if (matcher.find())
		{
		    output = matcher.group();
		}
		
		log("Done.");
		return output;
	}
	
	/**
	 * Gets the authentication code for betalands for the logged-in user.
	 * @return The betalands authentication code.
	 * @throws IOException
	 */
	private String getBetalandsAuth() throws IOException
	{
		ensureLogin();
		
		log("Getting Chatbox auth key...");
		String output = "";
		URL url = new URL("http://digibutter.nerr.biz/pages/betalands");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Document doc = Jsoup.parse(html);
		Element body = doc.getElementById("fullbody");
		Elements children = body.children();
		String src = "";
		for (Element e : children)
		{
			if (e.tagName().equalsIgnoreCase("a"))
			{
				src = e.attr("href");
			}
		}
		log(src);
		Pattern pattern = Pattern.compile("(?<=h\\=)\\w+");
		Matcher matcher = pattern.matcher(src);
		if (matcher.find())
		{
		    output = matcher.group();
		}
		
		log("Done.");
		return output;
	}
	
	/**
	 * Connects to the chatbox.
	 * @return The chat box object.
	 * @throws Exception
	 */
	public Chatbox getChat() throws Exception
	{
		return getChat(false);
	}
	
	/**
	 * Connects to the chatbox.
	 * @param debug Set to true to enable debug messages.
	 * @return The chat box object.
	 * @throws Exception
	 */
	public Chatbox getChat(boolean debug) throws Exception
	{
		return getChat(debug, null);
	}
	
	/**
	 * Connects to the chatbox.
	 * @param lastPath The path for reading and writing last seen user data.
	 * @return The chat box object.
	 * @throws Exception
	 */
	public Chatbox getChat(String lastPath) throws Exception
	{
		return getChat(false, lastPath);
	}
	
	/**
	 * Connects to the chatbox.
	 * @param debug Set to true to enable debug messages.
	 * @param lastPath The path for reading and writing last seen user data.
	 * @return The chat box object.
	 * @throws Exception
	 */
	public Chatbox getChat(boolean debug, String lastPath) throws Exception
	{
		ensureLogin();
		
		Date d = new Date();
		URL url = new URL("http://nerr.biz:8080/socket.io/1/?t=" + d.getTime());
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String result = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		String socketCode = result.split(":")[0];
		
		betalandsClient = new WebSocketClient();
		Chatbox chat = new Chatbox(getDisplayName(), getAvatarId(), getChatAuth(), debug, lastPath);
		
		betalandsClient.start();
		URI socketUri = new URI("ws://nerr.biz:8080/socket.io/1/websocket/" + socketCode);
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		betalandsClient.connect(chat, socketUri, request);
		log("Connecting to : " + socketUri);
		int timeout = 10;
		while (chat.isConnected() == false & timeout > 0)
		{
			Thread.sleep(1000);
			timeout--;
		}
		
		return chat;
	}
	
	/**
	 * Connects to betalands.
	 * @return The betalands object.
	 * @throws Exception
	 */
	public Betalands getBetalands() throws Exception
	{
		return getBetalands(false);
	}
	
	/**
	 * Connects to betalands.
	 * @param debug Set to true to enable debug messages.
	 * @return The betalands object.
	 * @throws Exception
	 */
	public Betalands getBetalands(boolean debug) throws Exception
	{
		return getBetalands(debug, null);
	}
	
	/**
	 * Connects to betalands.
	 * @param lastPath The path for reading and writing last seen user data.
	 * @return The betalands object.
	 * @throws Exception
	 */
	public Betalands getBetalands(String lastPath) throws Exception
	{
		return getBetalands(false, lastPath);
	}
	
	/**
	 * Connects to betalands.
	 * @param debug Set to true to enable debug messages.
	 * @param lastPath The path for reading and writing last seen user data.
	 * @return The betalands object.
	 * @throws Exception
	 */
	public Betalands getBetalands(boolean debug, String lastPath) throws Exception
	{
		ensureLogin();
		
		Date d = new Date();
		URL url = new URL("http://nerr.biz:8081/socket.io/1/?t=" + d.getTime());
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String result = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		String socketCode = result.split(":")[0];
		
		betalandsClient = new WebSocketClient();
		Betalands bLands = new Betalands(getDisplayName(), getAvatarId(), getBetalandsAuth(), debug, lastPath);
		
		betalandsClient.start();
		URI socketUri = new URI("ws://nerr.biz:8081/socket.io/1/websocket/" + socketCode);
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		betalandsClient.connect(bLands, socketUri, request);
		log("Connecting to : " + socketUri);
		int timeout = 10;
		while (bLands.isConnected() == false & timeout > 0)
		{
			Thread.sleep(1000);
			timeout--;
		}
		
		return bLands;
	}
	
	/**
	 * Gets the usernames of the five newest users to join the site.
	 * @return A list of usernames.
	 * @throws IOException
	 */
	public ArrayList<String> getNewUsers() throws IOException
	{
		log("Getting latest users..");
		ArrayList<String> output = new ArrayList<String>();
		URL url = new URL("http://digibutter.nerr.biz/");
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Cookie", cookieString());
		
		connection.connect();
		
		String html = getHtml(connection);
		updateCookies(connection);
		connection.disconnect();
		
		Document doc = Jsoup.parse(html);
		Elements list = doc.getElementById("bd").child(1).getElementsByClass("rightlist").get(3).getElementsByTag("li");
		for (int loop = 0; loop < list.size() - 1; loop++)
		{
			Element link = list.get(loop).getElementsByTag("a").get(0);
			String href = link.attr("href");
			String[] splitString = href.split("/");
			String newUsername = splitString[splitString.length - 1];
			output.add(newUsername);
		}
		
		return output;
	}

	/**
	 * Get the output of the latest HTTP response.
	 * @param connection The HTTP connection.
	 * @return The text returned by the server.
	 * @throws IOException
	 */
	private String getHtml(HttpURLConnection connection) throws IOException {
		InputStream input = connection.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String html = "";
		String line = "";
		while ((line = reader.readLine()) != null)
		{
			html += line;
		}
		return html;
	}
	
	/**
	 * Updates the cookie data from the last HTTP response.
	 * @param connection The HTTP connection.
	 */
	private void updateCookies(HttpURLConnection connection)
	{
		for (int loop = 1; connection.getHeaderFieldKey(loop) != null; loop++)
		{
			//log(connection.getHeaderFieldKey(loop) + "=" + connection.getHeaderField(loop));
			if (connection.getHeaderFieldKey(loop).equals("Set-Cookie"))
			{
				//log(connection.getHeaderField(loop));
				String[] splitString = connection.getHeaderField(loop).split(";");
				String[] cookie = splitString[0].split("=");
				cookies.put(cookie[0], cookie[1]);
			}
		}
	}
	
	/**
	 * Assembles the string of cookies to send to the server.
	 * @return The assembled cookie header.
	 */
	private String cookieString()
	{
		String output = "";
		for (Entry<String, String> cookie : cookies.entrySet())
		{
			output += cookie.getKey() + "=" + cookie.getValue() + "; ";
		}
		if (output.length() >= 2)
		{
			output.substring(0, output.length() - 2);
		}
		return output;
	}
	
	/**
	 * Prints a message if debugging is enabled.
	 * @param message The message to print.
	 */
	private void log(String message)
	{
		if (debug == true)
		{
			System.out.println("[" + new Date().toString() + "] [API] " + message);
		}
	}
	
	/**
	 * Clean up the chatbox connection.
	 * @throws Exception
	 */
	public void dispose() throws Exception
	{
		betalandsClient.stop();
		betalandsClient.destroy();
		betalandsClient = null;
	}
	
	/**
	 * Check if the user is currently logged in.
	 * @return True if the user is logged in, false if not.
	 */
	public boolean loggedIn()
	{
		if (cookies.containsKey("authautologin"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Log in to the website if the user is not logged in already.
	 * @throws IOException
	 */
	public void ensureLogin() throws IOException
	{
		if (loggedIn() == false)
		{
			login();
		}
	}
}
