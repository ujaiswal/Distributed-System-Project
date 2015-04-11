package hello;

import java.util.*;
import java.io.*;
import java.lang.*;
import java.net.*;

import hello.ParentServerApi;
import hello.MasterServerApi;

// import hello.Video;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.annotation.PostConstruct;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.scheduling.annotation.Scheduled;

import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;

// Tell Spring that this class is a Controller that should 
// handle certain HTTP requests for the DispatcherServlet
@Controller
public class Server implements ParentServerApi	{
	public static final String CHECK_SERVER = "/check"; //any
	public static final String VIDEO_SEARCH_PATH = "/video/search"; //get query
	public static final String VIDEO_ADD_PATH = "/video/add";  //put
	public static final String VIDEO_REMOVE_PATH = "/video/remove"; //delete
	public static final String CLIENT_CONNECT_PATH = "/client/connect"; //post
	public static final String CLIENT_DISCONNECT_PATH = "/client/disconnect"; //delete
	public static final String CLIENT_REQUEST_DELETE_PATH = "/client/askDelete"; //not implimented
	public static final String CLIENT_HEARTBEAT_PATH = "/client/heartbeat"; //amy

	public static final String CHECK_SERVER_RESPONSE = "Replicated System Server Running";
	public static final int CLIENT_ALREADY_CONNECTED = 102;
	public static final int CLIENT_CONNECTED = 101;
	public static final int FAILED = 401;	
	public static final int ACK = 100;
	public static final int PS_NOT_CONNECTED = -401;

	public static final int TTL = 60*1000; //60 seconds
	public static final int TTL1 = 10*1000; //60 seconds
	public static final int SEND_HEART_BEAT_DELAY = TTL1/3;
	public static final int SCHEDULED_CHECK = 100*1000; //10 seconds

	// An in-memory list that the servlet uses to store the
	// videos that are sent to it by clients
	private Map<String,Set<String> > vidName_UserMap = new HashMap<String,Set<String> > ();
	private Map<String,Set<String> > user_vidNameMap = new HashMap<String,Set<String> > ();	//only users connected on this PS
	private Set<String> activeUsers;
	private Map<String,Long> userAliveMap = new TreeMap<String,Long >();
	private static String hostAdder;
	// private Set<User> users = new TreeSet<User>();

	private final String TEST_URL = "http://localhost:9000";

	private MasterServerApi masterService = new RestAdapter.Builder()
			.setEndpoint(TEST_URL).setLogLevel(LogLevel.FULL).build()
			.create(MasterServerApi.class);

	// @Scheduled(fixedDelay = 2000 )
	// public void test()
	// {
	// 	String[] vid = {"abc","def","xyz"};
	// 	String ps = "10.3.100.201";
	// 	String user = "192.168.1.1";
	// 	masterService.psConnectClient(ps,user,vid);
	// }


	@RequestMapping(value=PS_DELETE_VIDEO, method=RequestMethod.GET)
	public @ResponseBody  int ps_DeleteVideo(@RequestParam(USER_PARAMETER) String user, @RequestParam(VIDEO_PARAMETER) String video){
		try{
			removeFromvidName_UserMap(user,video);
			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(""+e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=PS_DELETE_CLIENT, method=RequestMethod.GET)
	public @ResponseBody  int ps_DeleteClient(@RequestParam(USER_PARAMETER) String user){
		try{
			activeUsers.remove(user);
			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(""+e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value = CHECK_SERVER)
	public @ResponseBody String serverStatus()
	{
		return CHECK_SERVER_RESPONSE;
	}

	@RequestMapping(value=VIDEO_SEARCH_PATH, method=RequestMethod.GET)
	public @ResponseBody String[] searchVideo(@RequestParam(value="username") String uName,@RequestParam(value="video") String videoHash,HttpServletResponse response){
		System.out.println("Search from:" +uName);
		if(!vidName_UserMap.containsKey(videoHash))
		{
			response.setStatus(402); //client not connected
			return null;
		}

		Set<String> users = vidName_UserMap.get(videoHash);
		Iterator<String> it = users.iterator();
		while(it.hasNext())
		{
			String temp = it.next();
			if(!activeUsers.contains(temp))
			{
				it.remove();				
			}
		}
		if(users==null)
		{
			try{
				users = masterService.psSearch(hostAdder,videoHash);
			}
			catch(Exception e)
			{
				System.err.println(e.getMessage());
				return null;
			}
			if(users==null) return null;
			vidName_UserMap.get(videoHash).addAll(users);
		}
		System.out.println("Search result : "+ users.toArray(new String[0]) );
		// String [] a = new String[]
		return users.toArray(new String[0]);
	}
	
	@RequestMapping(value=CLIENT_CONNECT_PATH,method=RequestMethod.POST)
	public @ResponseBody int connectClient(@RequestBody String allVid)
	{
		try{
			int reply = FAILED;
			String[] videos = allVid.split(",");
			String uName = videos[0].trim();
			videos =  java.util.Arrays.copyOfRange(videos, 1, videos.length);
			int ans =masterService.psConnectClient(hostAdder,uName,videos);
			while(ans == PS_NOT_CONNECTED) {
				reconnectToMS();
				ans = masterService.psConnectClient(hostAdder,uName,videos);
			}
			if(ans == FAILED) return FAILED;
			if(user_vidNameMap.containsKey(uName))
			{ 
				reply = CLIENT_ALREADY_CONNECTED;
			}
			else
			{
			 	reply = CLIENT_CONNECTED;
			 	user_vidNameMap.put(uName, new HashSet<String>());
			}	
			Set<String> vidSet = user_vidNameMap.get(uName);
			for (int i=0;i<videos.length ;i++ ) {
				String temp = videos[i].trim();
				if(!temp.equals(""))
				{
					vidSet.add(temp);
					addTovidName_UserMap(uName,temp);

				}
			}
			userAliveMap.put(uName, System.currentTimeMillis()+TTL);
			activeUsers.remove(uName);
			System.out.println("Clinet "+ uName + " connected");
			return reply;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=VIDEO_ADD_PATH, method=RequestMethod.PUT)
	public @ResponseBody int addVideo(@RequestParam(value="username") String uName,@RequestParam(value="video") String videoHash,HttpServletResponse response){
		try{

			if(!user_vidNameMap.containsKey(uName))
			{
				response.setStatus(402); //client not connected
				return 0;
			}
			int ans =masterService.psAddVideo(hostAdder,uName,videoHash);
			while(ans == PS_NOT_CONNECTED) {
				reconnectToMS();
				ans = masterService.psAddVideo(hostAdder,uName,videoHash);
			}
			if(ans == FAILED) return FAILED;
			videoHash = videoHash.trim();
			user_vidNameMap.get(uName).add(videoHash);
			addTovidName_UserMap(uName,videoHash);
			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=VIDEO_REMOVE_PATH, method=RequestMethod.DELETE)
	public @ResponseBody int deleteVideo(@RequestParam(value="username") String uName,@RequestParam(value="video") String videoHash,HttpServletResponse response){
		try{

			if(!user_vidNameMap.containsKey(uName))
			{
				response.setStatus(402); //client not connected
				return 0;
			}
			videoHash = videoHash.trim();
			int ans =masterService.psDeleteVideo(hostAdder,uName,videoHash);
			while(ans == PS_NOT_CONNECTED) {
				reconnectToMS();
				ans = masterService.psDeleteVideo(hostAdder,uName,videoHash);
			}
			if(ans == FAILED) return FAILED;
			user_vidNameMap.get(uName).remove(videoHash);
			removeFromvidName_UserMap(uName,videoHash);

			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=CLIENT_DISCONNECT_PATH, method=RequestMethod.DELETE)
	public @ResponseBody int removeClient(@RequestParam(value="username") String uName,HttpServletResponse response){
		try{
			if(!user_vidNameMap.containsKey(uName))
			{
				response.setStatus(402); //client not connected
				return 0;
			}
			removeUser(uName);
			userAliveMap.remove(uName);
			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=CLIENT_HEARTBEAT_PATH)
	public @ResponseBody int heartBeat(@RequestParam(value="username") String uName,HttpServletResponse response){
		try{
			if(!user_vidNameMap.containsKey(uName))
			{
				response.setStatus(402); //client not connected
				return 0;
			}
			userAliveMap.put(uName, System.currentTimeMillis()+TTL);
			return TTL;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=PS_DELETE_MULTI_CLIENT,method = RequestMethod.POST)
	public int ps_DeleteMultipleClient(@RequestBody String[] user)
	{
		try{
			for(int i=0;i<user.length;i++)
			{
				activeUsers.remove(user);
			}
			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@Scheduled(fixedDelay = SCHEDULED_CHECK )
	public void checkActiveClients()
	{
		Set<String> users = user_vidNameMap.keySet();
		Iterator<String> it = users.iterator();
		long time = System.currentTimeMillis();
		int count=0;
		while(it.hasNext())
		{
			count++;
			String user = it.next();
			if(userAliveMap.containsKey(user))
			{
				if(userAliveMap.get(user)<time)
				{
					removeUser(user);
					userAliveMap.remove(user);
				}
			}
			else
			{
				throw new RuntimeException("user in user-vid map but not in user-alive map");
			}
		}
		System.out.println("Debug: Scheduled Check count:"+count+" user count:"+user_vidNameMap.size());
		System.gc();
	}

	@Scheduled(fixedDelay =SEND_HEART_BEAT_DELAY)
	public void sendHeartBeat()
	{
		try{
			masterService.psHeartBeat(hostAdder);
		}
		catch(Exception e){
			System.out.println("Error Sending HeardBeat");
		}
		
	}

	private void reconnectToMS()
	{
		if(masterService.connectPS(hostAdder)==FAILED)
    	{
    		System.out.println("Could not connect to MS. Shutting down");
    		System.exit(0);
    	}

	}

	private void removeUser(String user)
	{
		int count =0;
		
			
		if(user_vidNameMap.containsKey(user))
		{
			Set<String> vids = user_vidNameMap.get(user);
			Iterator<String> it = vids.iterator();
			while(it.hasNext())
			{
				removeFromvidName_UserMap(user,it.next());
			}
			vids.clear();
			user_vidNameMap.remove(user);			
		}
		activeUsers.remove(user);
		int ans =masterService.psDisConnectClient(hostAdder,user);
		while(ans != ACK) {
			if(count>10) throw new RuntimeException("Time out in removing user from MS");
			reconnectToMS();
			ans = masterService.psDisConnectClient(hostAdder,user);
			count++;

		}
		System.out.println("User "+ user+" removed.");
	}

	private void addTovidName_UserMap(String user, String video)
	{
		if(vidName_UserMap.containsKey(video))
		{
			vidName_UserMap.get(video).add(user);
		}
		else
		{
			Set<String> s = new HashSet<String>();
			s.add(user);
			vidName_UserMap.put(video,s);
		}
	}

	private void removeFromvidName_UserMap(String user, String video)
	{
		if(vidName_UserMap.containsKey(video))
		{
			vidName_UserMap.get(video).remove(user);
			if(vidName_UserMap.get(video).isEmpty())
			{
				vidName_UserMap.remove(video);
			}
		}
	}

	@PostConstruct
	public void getHostAdderesAndConnectToMS()
	{
		try{
			Enumeration e = NetworkInterface.getNetworkInterfaces();
			while(e.hasMoreElements())
			{
			    NetworkInterface n = (NetworkInterface) e.nextElement();
			    Enumeration ee = n.getInetAddresses();
			    while (ee.hasMoreElements())
			    {
			        InetAddress i = (InetAddress) ee.nextElement();
			        if(i.getHostAddress().contains("10."))
			        {
			        	hostAdder = i.getHostAddress().trim();
			        	System.out.println("Run at start. IP:" + hostAdder);
			        	if(masterService.connectPS(hostAdder)==FAILED)
			        	{
			        		System.out.println("Could not connect to MS. Shutting down");
			        		System.exit(0);
			        	}
			        	return;
			        }
			    }
			}
		}
		catch(Exception e){}
	}
}
