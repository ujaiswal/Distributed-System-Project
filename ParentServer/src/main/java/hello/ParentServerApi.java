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
public interface ParentServerApi {
	
	public static final String CHECK_SERVER = "/check"; //any

	public static final String PS_DELETE_CLIENT = "/ms/delete/client";

	public static final String PS_DELETE_MULTI_CLIENT = "/ms/delete/multiclient";
	
	public static final String PS_DELETE_VIDEO = "/ms/delete/video";

	public static final String USER_PARAMETER = "user";

	public static final String VIDEO_PARAMETER = "video";

	// public static final String PS_PARAMETER = "user";

	// The path to search videos by title
	// public static final String VIDEO_TITLE_SEARCH_PATH = VIDEO_SVC_PATH + "/find";

	@DELETE(PS_DELETE_VIDEO)
	public int ps_DeleteVideo(@Query(USER_PARAMETER) String user, @Query(VIDEO_PARAMETER) String video);

	@DELETE(PS_DELETE_CLIENT)
	public int ps_DeleteClient(@Query(USER_PARAMETER) String user);

	@POST(PS_DELETE_MULTI_CLIENT)
	public int ps_DeleteMultipleClient(@Body String[] user);

	@GET(CHECK_SERVER)
	public String serverStatus();
}
