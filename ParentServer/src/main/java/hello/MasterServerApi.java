package hello;

import java.util.*;
import java.io.*;
import java.lang.*;

import retrofit.http.*;

/**
 * This interface defines an API for a VideoSvc. The
 * interface is used to provide a contract for client/server
 * interactions. The interface is annotated with Retrofit
 * annotations so that clients can automatically convert the
 * 
 * 
 * @author jules
 *
 */
public interface MasterServerApi {
	
	// public static final String PS_DELETE_CLIENT = "ms/delete/client";

	public static final String PS_CLIENT_CONNECT = "/ms/client/connect";

	public static final String PS_CLIENT_DISCONNECT = "/ms/client/disconnect";

	public static final String PS_VIDEO_ADD = "/ms/video/add";

	public static final String PS_VIDEO_DELETE = "/ms/video/delete";

	public static final String PS_VIDEO_SEARCH = "/ms/search";

	public static final String PS_CONNECT = "/ms/ps/connect";

	public static final String PS_HEARTBEAT = "/ms/ps/heartbeat";

	// public static final String PS_CLIENT_DISCONNECT = "ms/client/disconnect";

	public static final String USER_PARAMETER = "user";

	public static final String VIDEO_PARAMETER = "video";

	public static final String PS_PARAMETER = "ps";

	// public static final String PS_PARAMETER = "user";

	// The path to search videos by title
	// public static final String VIDEO_TITLE_SEARCH_PATH = VIDEO_SVC_PATH + "/find";

	@GET(PS_CONNECT)
	public int connectPS(@Query(PS_PARAMETER) String ps);

	@GET(PS_HEARTBEAT)
	public int psHeartBeat(@Query(PS_PARAMETER) String ps);

	@PUT(PS_VIDEO_ADD)
	public int psAddVideo(@Query(PS_PARAMETER) String ps, @Query(USER_PARAMETER) String user, @Query(VIDEO_PARAMETER) String video);

	@DELETE(PS_VIDEO_DELETE)
	public int psDeleteVideo(@Query(PS_PARAMETER) String ps, @Query(USER_PARAMETER) String user, @Query(VIDEO_PARAMETER) String video);

	@GET(PS_VIDEO_SEARCH)
	public Set<String> psSearch(@Query(PS_PARAMETER) String ps,@Query(VIDEO_PARAMETER) String video);

	@POST(PS_CLIENT_CONNECT)
	public int psConnectClient(@Query(PS_PARAMETER) String ps, @Query(USER_PARAMETER) String user, @Body String[] videos);

	@DELETE(PS_CLIENT_DISCONNECT)
	public int psDisConnectClient(@Query(PS_PARAMETER) String ps, @Query(USER_PARAMETER) String user);
	
}
