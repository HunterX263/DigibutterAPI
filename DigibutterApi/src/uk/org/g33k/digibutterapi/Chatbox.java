package uk.org.g33k.digibutterapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebSocket
public class Chatbox {
	private String username;
	private String avatar;
	private String auth;
	private Session session;
	private final CountDownLatch closeLatch;
	private boolean connected = false;
	private ArrayList<MessageListener> messageListeners = new ArrayList<MessageListener>();
	private ArrayList<JoinListener> joinListeners = new ArrayList<JoinListener>();
	private boolean debug = false;
	private HashMap<String, String> online = new HashMap<String, String>();
	private HashMap<String, Date> lastSeen = new HashMap<String, Date>();
	private String lastPath;
	
	public interface MessageListener
	{
		public void onMessage(String username, String message);
	}
	
	public interface JoinListener
	{
		public void onJoin(String username);
	}
	
	public Chatbox(String username, String avatar, String auth, boolean debug, String lastPath)
	{
		this.username = username;
		this.avatar = avatar;
		this.auth = auth;
		this.closeLatch = new CountDownLatch(1);
		this.debug = debug;
		if (lastPath != null)
		{
			this.lastPath = lastPath;
			try {
				lastSeen = loadLastSeen(lastPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public Chatbox(String username, String avatar, String auth, boolean debug)
	{
		this.username = username;
		this.avatar = avatar;
		this.auth = auth;
		this.closeLatch = new CountDownLatch(1);
		this.debug = debug;
	}
	
	public Chatbox(String username, String avatar, String auth)
	{
		this.username = username;
		this.avatar = avatar;
		this.auth = auth;
		this.closeLatch = new CountDownLatch(1);
	}
	
	/**
	 * Wait for the connection to close, or until the time expires.
	 * @param duration The time to wait.
	 * @param unit The unit of time.
	 * @return True if disconnected, false if the time ran out before disconnecting.
	 * @throws InterruptedException
	 */
	public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
	
	/**
	 * Wait for the connection to close.
	 * @throws InterruptedException
	 */
	public void awaitClose() throws InterruptedException
	{
		closeLatch.await();
	}
	
	/**
	 * Called when the websocket connection closes.
	 * @param statusCode The status code returned.
	 * @param reason The reason for the disconnect.
	 */
	@OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log("Connection closed: " + statusCode + " - " + reason);
        this.session = null;
        this.closeLatch.countDown();
        connected = false;
    }
 
	/**
	 * Called when the websocket connection opens.
	 * @param session The session for this connection.
	 */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        log("Got connect: " + session);
        this.session = session;
        try {
            Future<Void> fut;
            fut = session.getRemote().sendStringByFuture("5:::{\"name\":\"adduser\",\"args\":[\"" + username + "\",\"" + avatar + "\",\"" + auth + "\"]}");
            fut.get(2, TimeUnit.SECONDS);
            connected = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
 
    /**
     * Called when the client recieves a message from the server.
     * @param msg The message recieved.
     */
    @OnWebSocketMessage
    public void onMessage(String msg) {
        log("Got msg: " + msg);
        if (msg.equals("2::"))
        {
        	try {
        		Future<Void> fut;
        		fut = session.getRemote().sendStringByFuture("2::");
        		fut.get(2, TimeUnit.SECONDS);
        	} catch (Throwable t) {
        		t.printStackTrace();
        	}
        }
        else if (msg.startsWith("5:::"))
        {
        	try {
        		String jMsg = msg.substring(4);
				JSONObject json = (JSONObject)new JSONParser().parse(jMsg);
				if (json.get("name").equals("updatechat") | json.get("name").equals("refreshchat"))
				{
					JSONArray args = (JSONArray)json.get("args");
					JSONObject object = (JSONObject)args.get(0);
					String mUsername = (String)object.get("username");
					String message = (String)object.get("content");
					lastSeen.put(mUsername, new Date());
					try {
						saveLastSeen(lastPath, lastSeen);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (json.get("name").equals("refreshchat"))
					{
						String[] splitMessage = message.split("\n");
						message = splitMessage[splitMessage.length - 1];
					}
					if (this.username.equals(mUsername) == false)
					{
						for (MessageListener m : messageListeners)
						{
							m.onMessage(mUsername, message);
						}
					}
				}
				else if (json.get("name").equals("updateusers"))
				{
					HashMap<String, String> oldOnline = new HashMap<String, String>(online);
					online.clear();
					Date current = new Date();
					JSONArray array = (JSONArray)json.get("args");
					JSONArray args = (JSONArray)array.get(0);
					boolean lastUpdated = false;
					for (int loop = 0; loop < args.size(); loop++)
					{
						JSONObject object = (JSONObject)args.get(loop);
						String mUsername = (String)object.get("username");
						String mAvatar = (String)object.get("avatar");
						online.put(mUsername, mAvatar);
						if (oldOnline.containsKey(object.get("username")) == false)
						{
							lastSeen.put(mUsername, current);
							lastUpdated = true;
							for (JoinListener j : joinListeners)
							{
								j.onJoin(mUsername);
							}
						}
					}
					if (lastUpdated == true)
					{
						try {
							saveLastSeen(lastPath, lastSeen);
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }
    
    /**
     * Send a new message to the chat.
     * @param msg The message to send.
     */
    public void postMessage(String msg)
    {
    	log("Posting message: " + msg);
    	try {
    		Future<Void> fut;
    		fut = session.getRemote().sendStringByFuture("5:::{\"name\":\"sendchat\",\"args\":[{\"content\":\"" + msg + "\",\"inReplyTo\":0}]}");
    		fut.get(2, TimeUnit.SECONDS);
    	} catch (Throwable t) {
    		t.printStackTrace();
    	}
    }
    
    /**
     * Close the chat box connection.
     * @param duration The maximum time to wait for the chat to close.
     * @param unit The unit of time to wait.
     * @throws InterruptedException
     */
    public void close(int duration, TimeUnit unit) throws InterruptedException
    {
    	session.close(StatusCode.NORMAL, null);
    	awaitClose(duration, unit);
    }
    
    /**
     * Checks if the chat is connected.
     * @return True if connected, false otherwise.
     */
    public boolean isConnected()
    {
    	return connected;
    }
    
    /**
     * Add a listener for the onMessage event.
     * @param toAdd The listener to add.
     */
    public void addOnMessageListener(MessageListener toAdd)
    {
    	messageListeners.add(toAdd);
    }
    
    /**
     * Add a listener for the onJoin event.
     * @param toAdd The listener to add.
     */
    public void addOnJoinListener(JoinListener toAdd)
    {
    	joinListeners.add(toAdd);
    }
    
    /**
     * Get the list of currently online users.
     * @return A list of usernames and their associated avatar IDs.
     */
    public HashMap<String, String> getOnline()
    {
    	return new HashMap<String, String>(online);
    }
    
    /**
     * Get the list of when each user was last seen.
     * @return A list of usernames and the date that username last connected or posted.
     */
    public HashMap<String, Date> getLastSeen()
    {
    	return new HashMap<String, Date>(lastSeen);
    }
    
    /**
     * If debugging is enabled, print a message.
     * @param message The message to print.
     */
    private void log(String message)
	{
		if (debug == true)
		{
			System.out.println("[Chatbox][" + new Date().toString() + "] " + message);
		}
	}
    
    /**
     * Load the file of when users were last seen.
     * @param path The path to the file.
     * @return A HashMap of usernames and the dates they were last seen.
     * @throws IOException
     */
    private HashMap<String, Date> loadLastSeen(String path) throws IOException
	{	
    	log("Loading last seen file.");
		HashMap<String, Date> output = new HashMap<String, Date>();
		
		File lastFile = new File(path);
		if (lastFile.exists() == true & lastFile.isFile())
		{
			BufferedReader reader = new BufferedReader(new FileReader(lastFile));
			String username = reader.readLine();
			String timestamp = reader.readLine();
			while (timestamp != null)
			{
				output.put(username, new Date(Long.parseLong(timestamp)));
				username = reader.readLine();
				timestamp = reader.readLine();
			}
			reader.close();
		}
		
		return output;
	}
	
    /**
     * Save the last seen list to file.
     * @param path The path to the file.
     * @param lastSeen The list to save.
     * @throws FileNotFoundException
     */
	private void saveLastSeen(String path, HashMap<String, Date> lastSeen) throws FileNotFoundException
	{
		log("Saving last seen file.");
		PrintWriter writer = new PrintWriter(path);
		for (Entry<String, Date> e : lastSeen.entrySet())
		{
			writer.println(e.getKey());
			writer.println(e.getValue().getTime());
		}
		writer.close();
	}
}
