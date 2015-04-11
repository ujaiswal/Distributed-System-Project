package hello;

import java.util.*;
import java.io.*;
import java.lang.*;

import hello.MasterServerApi;
import hello.ParentServerApi;

// import hello.Video;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

class PSInfo{
	public static String port ="8080";
	public String name;
	public Set<String> users;
	public long ttl;
	public ParentServerApi parentService;
	public PSInfo(String name, Set<String> users, long ttl)
	{
		this.name = name;
		this.users = users;
		this.ttl = ttl;
		parentService = new RestAdapter.Builder()
			.setEndpoint("http://"+name+":"+port).setLogLevel(LogLevel.FULL).build()
			.create(ParentServerApi.class);
	}
}
// Tell Spring that this class is a Controller that should 
// handle certain HTTP requests for the DispatcherServlet
@Controller
public class Server implements MasterServerApi {
	// public static final String CHECK_SERVER = "/check"; //any
	// public static final String VIDEO_SEARCH_PATH = "/video/search"; //get query
	// public static final String VIDEO_ADD_PATH = "/video/add";  //put
	// public static final String VIDEO_REMOVE_PATH = "/video/remove"; //delete
	// public static final String CLIENT_CONNECT_PATH = "/client/connect"; //post
	// public static final String CLIENT_DISCONNECT_PATH = "/client/disconnect"; //delete
	// public static final String CLIENT_REQUEST_DELETE_PATH = "/client/askDelete"; //not implimented
	// public static final String CLIENT_HEARTBEAT_PATH = "/client/heartbeat"; //amy

	public static final String CHECK_SERVER_RESPONSE = "Replicated System Server Running";
	public static final int CLIENT_ALREADY_CONNECTED = 102;
	public static final int CLIENT_CONNECTED = 101;
	public static final int PS_NOT_CONNECTED = -401;


	public static final String CLIENT_ASK_PS_PATH = "/client/askPS"; //get

	public static final int FAILED = -1;
	public static final int ACK = 100;
	public static final int TTL1 = 10000*1000; //60 seconds
	public static final int SCHEDULED_CHECK = 2*1000; //10 seconds

	// An in-memory list that the servlet uses to store the
	// videos that are sent to it by clients
	private Map<String,Set<String> > vidName_UserMap = new HashMap<String,Set<String> > ();
	private Map<String,Set<String> > user_vidNameMap = new HashMap<String,Set<String> > ();
	// private Map<String,Long> userAliveMap = new TreeMap<String,Long >();
	// private Map<String,Set<String> > ps_userMap = new HashMap<String,Set<String> > ();
	private Map<String,PSInfo> ps_infoMap = new  HashMap<String,PSInfo> ();
	// private Map<String,ParentServerApi> ps_psServerMap = new  HashMap<String,ParentServerApi> ();
	private TreeSet<String> allPsSet = new TreeSet<String> (); //may not be needed
	// private Set<User> users = new TreeSet<User>();


	// @RequestMapping(value=PS_CLIENT_CONNECT,method=RequestMethod.POST)
	// public @ResponseBody int psConnectClient(@RequestParam(PS_PARAMETER) String ps, @RequestParam(USER_PARAMETER) String user, @RequestBody String[] videos)
	// {
	// 	System.out.println("New Request");
	// 	System.out.println(ps+" "+user+" "+ Arrays.asList(videos));
	// 	return ACK;
	// }

	@RequestMapping(value=PS_CONNECT,method=RequestMethod.GET)
	public @ResponseBody int connectPS(@RequestParam(PS_PARAMETER) String ps){
		try{
			if(ps_infoMap.containsKey(ps))
			{
				ps_infoMap.get(ps).ttl = System.currentTimeMillis()+TTL1;
				return CLIENT_ALREADY_CONNECTED;
			}
			else
			{
				ps_infoMap.put(ps,new PSInfo(ps,new HashSet<String>(),System.currentTimeMillis()+TTL1));
				return CLIENT_CONNECTED;
			}
		}
		catch(Exception e)
		{
			System.err.println(""+e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=PS_HEARTBEAT,method=RequestMethod.GET)
	public @ResponseBody int psHeartBeat(@RequestParam(PS_PARAMETER) String ps){
		// System.out.println("RECIEVED_HEARTBEAT");
		if(ps_infoMap.containsKey(ps))
		{
			ps_infoMap.get(ps).ttl = System.currentTimeMillis()+TTL1;
			return TTL1;
		}
		else
		{
			return PS_NOT_CONNECTED;
		}
	}

	@RequestMapping(value=PS_VIDEO_ADD,method=RequestMethod.PUT)
	public @ResponseBody int psAddVideo(@RequestParam(PS_PARAMETER) String ps, @RequestParam(USER_PARAMETER) String user, @RequestParam(VIDEO_PARAMETER) String video){
		try{
			if(!user_vidNameMap.containsKey(user))
			{
				return FAILED;
			}
			video = video.trim();
			user_vidNameMap.get(user).add(video);
			addTovidName_UserMap(user,video);
			return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	
	}

	@RequestMapping(value=PS_VIDEO_DELETE,method=RequestMethod.DELETE)
	public @ResponseBody int psDeleteVideo(@RequestParam(PS_PARAMETER) String ps, @RequestParam(USER_PARAMETER) String user, @RequestParam(VIDEO_PARAMETER) String video){
		try{
			Collection<PSInfo> allps = ps_infoMap.values();
			Iterator<PSInfo> it= allps.iterator();
			while(it.hasNext())
			{
				PSInfo temp = it.next();
				if(!temp.name.equals(ps))
				{
					try{
						if(temp.parentService.ps_DeleteVideo(user,video)==FAILED) return FAILED;
					}
					catch(Exception e)
					{
						removePS(temp);
					}
				}

			}
			user_vidNameMap.get(user).remove(video);
	 		removeFromvidName_UserMap(user,video);
	 		return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=PS_VIDEO_SEARCH,method=RequestMethod.GET)
	public @ResponseBody Set<String>  psSearch(@RequestParam(PS_PARAMETER) String ps,@RequestParam(VIDEO_PARAMETER) String video){
		Set<String> users = vidName_UserMap.get(video);
		//TODO check is following line gives error in null
		System.out.println("Search : "+users.size());
		if(users.size()==0) return null;
		// System.out.println("Search result : "+ Arrays.asList(users.toArray(new String[0])) );
		// String [] a = new String[]
		return users;
	}

	@RequestMapping(value=PS_CLIENT_CONNECT,method=RequestMethod.POST)
	public @ResponseBody int psConnectClient(@RequestParam(PS_PARAMETER) String ps, @RequestParam(USER_PARAMETER) String user, @RequestBody String[] videos){
		try{
			System.out.println("Trying connect client");
			if(!ps_infoMap.containsKey(ps)) return PS_NOT_CONNECTED;
			int reply;
			if(user_vidNameMap.containsKey(user))
			{ 
				reply = CLIENT_ALREADY_CONNECTED;
			}
			else
			{
			 	reply = CLIENT_CONNECTED;
			 	user_vidNameMap.put(user, new HashSet<String>());
			}	
			Set<String> vidSet = user_vidNameMap.get(user);
			for (int i=0;i<videos.length ;i++ ) {
				String temp = videos[i].trim();
				if(!temp.equals(""))
				{
					vidSet.add(temp);
					addTovidName_UserMap(user,temp);

				}
			}
			ps_infoMap.get(ps).users.add(user);
			System.out.println("Clinet "+ user + " connected");
			return reply;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}

	@RequestMapping(value=PS_CLIENT_DISCONNECT,method=RequestMethod.DELETE)
	public @ResponseBody int psDisConnectClient(@RequestParam(PS_PARAMETER) String ps, @RequestParam(USER_PARAMETER) String user){
		try{
			Collection<PSInfo> allps = ps_infoMap.values();
			Iterator<PSInfo> it= allps.iterator();
			while(it.hasNext())
			{
				PSInfo temp = it.next();
				if(!temp.name.equals(ps))
				{
					try{
						if(temp.parentService.ps_DeleteClient(user)==FAILED) return FAILED;
					}
					catch(Exception e)
					{
						removePS(temp);
					}
				}
			}
			removeUser(user);
			ps_infoMap.get(ps).users.remove(user);
	 		return ACK;
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			return FAILED;
		}
	}
	
	// @RequestMapping(value=PS_DELETE_VIDEO, method=RequestMethod.GET)
	// public int PS_DeleteVideo(@RequestParam(USER_PARAMETER) String user, @RequestParam(VIDEO_PARAMETER) String video){
	// 	try{
	// 		user_vidNameMap.get(user).remove(video);
	// 		removeFromvidName_UserMap(user,video);
	// 		return ACK;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(""+e.getMessage());
	// 		return FAILED;
	// 	}
	// }

	// @RequestMapping(value=PS_DELETE_CLIENT, method=RequestMethod.GET)
	// public int PS_DeleteClient(@RequestParam(USER_PARAMETER) String user){
	// 	try{
	// 		removeUser(user);
	// 		return ACK;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(""+e.getMessage());
	// 		return FAILED;
	// 	}
	// }

	// @RequestMapping(value = CHECK_SERVER)
	// public @ResponseBody String serverStatus()
	// {
	// 	return CHECK_SERVER_RESPONSE;
	// }

	// @RequestMapping(value=VIDEO_SEARCH_PATH, method=RequestMethod.GET)
	// public @ResponseBody String[] searchVideo(@RequestParam(value="username") String uName,@RequestParam(value="video") String videoHash,HttpServletResponse response){
	// 	System.out.println("Search from:" +uName);
	// 	if(!vidName_UserMap.containsKey(videoHash))
	// 	{
	// 		response.setStatus(402); //client not connected
	// 		return null;
	// 	}

	// 	Set<String> users = vidName_UserMap.get(videoHash);
	// 	if(users==null) return null;
	// 	System.out.println("Search result : "+ users.toArray(new String[0]) );
	// 	// String [] a = new String[]
	// 	return users.toArray(new String[0]);
	// }
	
	// @RequestMapping(value=CLIENT_CONNECT_PATH,method=RequestMethod.POST)
	// public @ResponseBody int connectClient(@RequestBody String allVid)
	// {
	// 	try{
	// 		int reply = FAILED;
	// 		String[] videos = allVid.split(",");
	// 		String uName = videos[0].trim();
	// 		if(user_vidNameMap.containsKey(uName))
	// 		{ 
	// 			reply = CLIENT_ALREADY_CONNECTED;
	// 		}
	// 		else
	// 		{
	// 		 	reply = CLIENT_CONNECTED;
	// 		 	user_vidNameMap.put(uName, new HashSet<String>());
	// 		}	
	// 		Set<String> vidSet = user_vidNameMap.get(uName);
	// 		for (int i=1;i<videos.length ;i++ ) {
	// 			String temp = videos[i].trim();
	// 			if(!temp.equals(""))
	// 			{
	// 				vidSet.add(temp);
	// 				addTovidName_UserMap(uName,temp);

	// 			}
	// 		}
	// 		userAliveMap.put(uName, System.currentTimeMillis()+TTL1);
	// 		System.out.println("Clinet "+ uName + " connected");
	// 		return reply;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(e.getMessage());
	// 		return FAILED;
	// 	}
	// }

	// @RequestMapping(value=VIDEO_ADD_PATH, method=RequestMethod.PUT)
	// public @ResponseBody int addVideo(@RequestParam(value="username") String uName,@RequestParam(value="video") String videoHash,HttpServletResponse response){
	// 	try{

	// 		if(!user_vidNameMap.containsKey(uName))
	// 		{
	// 			response.setStatus(402); //client not connected
	// 			return 0;
	// 		}
	// 		videoHash = videoHash.trim();
	// 		user_vidNameMap.get(uName).add(videoHash);
	// 		addTovidName_UserMap(uName,videoHash);
	// 		return ACK;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(e.getMessage());
	// 		return FAILED;
	// 	}
	// }

	// @RequestMapping(value=VIDEO_REMOVE_PATH, method=RequestMethod.DELETE)
	// public @ResponseBody int deleteVideo(@RequestParam(value="username") String uName,@RequestParam(value="video") String videoHash,HttpServletResponse response){
	// 	try{

	// 		if(!user_vidNameMap.containsKey(uName))
	// 		{
	// 			response.setStatus(402); //client not connected
	// 			return 0;
	// 		}
	// 		videoHash = videoHash.trim();
	// 		user_vidNameMap.get(uName).remove(videoHash);
	// 		removeFromvidName_UserMap(uName,videoHash);
	// 		return ACK;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(e.getMessage());
	// 		return FAILED;
	// 	}
	// }

	// @RequestMapping(value=CLIENT_DISCONNECT_PATH, method=RequestMethod.DELETE)
	// public @ResponseBody int removeClient(@RequestParam(value="username") String uName,HttpServletResponse response){
	// 	try{
	// 		if(!user_vidNameMap.containsKey(uName))
	// 		{
	// 			response.setStatus(402); //client not connected
	// 			return 0;
	// 		}
	// 		removeUser(uName);
	// 		userAliveMap.remove(uName);
	// 		return ACK;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(e.getMessage());
	// 		return FAILED;
	// 	}
	// }

	// @RequestMapping(value=CLIENT_HEARTBEAT_PATH)
	// public @ResponseBody int heartBeat(@RequestParam(value="username") String uName,HttpServletResponse response){
	// 	try{
	// 		if(!user_vidNameMap.containsKey(uName))
	// 		{
	// 			response.setStatus(402); //client not connected
	// 			return 0;
	// 		}
	// 		userAliveMap.put(uName, System.currentTimeMillis()+TTL1);
	// 		return TTL1;
	// 	}
	// 	catch(Exception e)
	// 	{
	// 		System.err.println(e.getMessage());
	// 		return FAILED;
	// 	}
	// }



	@Scheduled(fixedDelay = SCHEDULED_CHECK )
	public void checkPerodicActivePS()	{
		try{
			Collection<PSInfo> allps = ps_infoMap.values();
			Iterator<PSInfo> it= allps.iterator();
			long time = System.currentTimeMillis();
			// System.out.println("Time:"+time+" "+(time+TTL1));
			int count=0;
			while(it.hasNext())
			{
				PSInfo temp = it.next();
				if(temp.ttl<time)
				{
					removePS(temp);
				}
			}
			// System.out.println("Debug: Scheduled Check count:"+count+" user count:"+ps_infoMap.size());
			System.gc();
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			
		}
	}
	
	private void checkActivePS()	{
		try{
			Collection<PSInfo> allps = ps_infoMap.values();
			Iterator<PSInfo> it= allps.iterator();
			long time = System.currentTimeMillis();
			int count=0;
			while(it.hasNext())
			{
				PSInfo temp = it.next();
				try{
					temp.parentService.serverStatus();
				}
				catch(Exception e)
				{
					removePS(temp);
				}
			}
			System.out.println("Debug: Scheduled Check count:"+count+" user count:"+user_vidNameMap.size());
			System.gc();
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			
		}
	}

	private void removePS(PSInfo psinfo)
	{
		System.out.println("Removing user"+psinfo.name);
		Collection<PSInfo> allps = ps_infoMap.values();
		Iterator<PSInfo> it= allps.iterator();
		String[] allUsers = psinfo.users.toArray(new String[0]);
		while(it.hasNext())
		{
			PSInfo temp = it.next();
			if(!temp.name.equals(psinfo.name))
			{
				try{
				temp.parentService.ps_DeleteMultipleClient(allUsers);
				}
				catch (Exception e)
				{
					System.out.println("PS not responding:"+temp.name);
				}
			}
		}
		ps_infoMap.get(psinfo.name).users.clear();
		ps_infoMap.remove(psinfo.name);
 				
	}

	private void removeUser(String user)
	{
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

	@RequestMapping(value=CLIENT_ASK_PS_PATH, method=RequestMethod.GET)
	public @ResponseBody List<String> getPSList()
	{
		//TODO check this fn;
		List<String> allPS =Arrays.asList(ps_infoMap.keySet().toArray(new String[0]));
		System.out.println("Before:"+(allPS));
		Collections.sort(allPS,  new Comparator<String>() 
                           {
                            public int compare(String o1, String o2) 
                            {
                               return ps_infoMap.get(o1).users.size() - (ps_infoMap.get(o2).users.size());
                            }
                           }  );
		System.out.println("After:"+(allPS));
		return allPS;
	}
}
