import os
import sys
import socket
import requests
import urllib
from bs4 import BeautifulSoup
from subprocess import call
import re
import threading
import time
import json

DEBUG = '[R-Youtube] '

YOUTUBE_VIDEO = 1
SERVER_VIDEO = 2

# Required to set manually
Master_Server_IP_Address = '192.168.150.4'
Master_Server_Port = '9000'

Client_IP_Address = '0.0.0.0'
Client_Port = '8000'

Server_IP_Address_List = {}
Current_index = 0
IsFirstConnect = True

Server_IP_Address = '0.0.0.0'
Server_Port = '8080'

Local_IP_Address = 'http://localhost:8000'
Youtube_Address = 'https://youtube.com'

Youtube_Video_Address = 'https://www.youtube.com/watch?v='
Youtube_Search_Path = 'https://www.youtube.com/results?search_query='
Current_folder = os.path.abspath(os.path.dirname(__file__))


Downloads = []
Download_Lock = threading.Lock()
def P():
	global Download_Lock
	Download_Lock.acquire()

def V():
	global Download_Lock
	Download_Lock.release()

hb_thread = None
class HEARTBEAT_Thread (threading.Thread):
	def __init__(self, threadID, TTL):
		threading.Thread.__init__(self)
		self.threadID = threadID
		self.TTL = TTL

	def stop(self):
		self._stop.set()

	def stopped(self):
		return self._stop.isSet()

	def run(self):
		startSendingHEARTBEATToParentServer(self.TTL)

class FILE_CHK_Thread (threading.Thread):
	def __init__(self, threadID, delay):
		threading.Thread.__init__(self)
		self.threadID = threadID
		self.delay = delay

	def run(self):
		video_list = getVideoList()
		prev_video_list = set(video_list)
		while 1:
			time.sleep(self.delay)
			curr_video_list = set(getVideoList())
			
			addititons = curr_video_list.difference(prev_video_list)
			if len(addititons) > 0 :
				print DEBUG + 'Sending additions to ' + Server_IP_Address
				params = { 
					'username' : Client_IP_Address,
				}
				for e in addititons :
					params['video'] = str(e)
					proceed = False
					while not proceed:
						try: 
							response = requests.put('http://' + Server_IP_Address + ':' + Server_Port + '/video/add', params=params )
							if response.ok :
								proceed = True
						except:
							startConnect()
			
			deletions = prev_video_list.difference(curr_video_list)
			if len(deletions) > 0 :
				print DEBUG + 'Sending deletions to ' + Server_IP_Address
				params = { 
					'username' : Client_IP_Address,
				}
				for e in deletions :
					params['video'] = str(e)
					proceed = False
					while not proceed:
						try: 
							response = requests.delete('http://' + Server_IP_Address + ':' + Server_Port + '/video/remove', params=params )
							if response.ok :
								proceed = True
						except:
							startConnect()

			prev_video_list = curr_video_list

class DOWNLOAD_Thread(threading.Thread):
	def __init__(self, threadID, video_id, case, video_loc):
		threading.Thread.__init__(self)
		self.threadID = threadID
		self.videoID = video_id
		self.case = case
		self.videoLOC = video_loc

	def run(self):
		P()
		global Downloads
		if self.videoID in Downloads :
			V()
			return
		else :
			Downloads.append(self.videoID)
			V()

		if self.case == YOUTUBE_VIDEO:
			return_code = downloadFileFromYoutube(self.videoID)
		elif self.case == SERVER_VIDEO:
			return_code = downloadFileFromClient(self.videoID,self.videoLOC)

		P()
		Downloads.remove(self.videoID)
		V()

# Extratcs the IP Address of the client
def extractClientIP():
	s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	s.connect(('youtube.com',80))
	ip = s.getsockname()[0]
	s.close()
	return ip

# Sets the global variable Client_IP_Address
def setClientIP():
	global Client_IP_Address
	Client_IP_Address = extractClientIP()
	print DEBUG + 'Client IP Address: ' + Client_IP_Address

# Get the best server from master server on startup
def  getServerIPsfromMaster():
	global Current_index
	global IsFirstConnect
	Current_index = 0
	IsFirstConnect = True

	try :
		response = requests.get('http://' + Master_Server_IP_Address + ':' + Master_Server_Port + '/client/askPS')	
		if response.ok :
			global Server_IP_Address_List
			Server_IP_Address_List = json.loads(response.text)
	except :
		pass

# Sets the global variable Server_IP_Address
def setServerIP():
	global Server_IP_Address_List
	if Server_IP_Address_List :
		global Server_IP_Address
		global Current_index
		if Current_index == 0 and not IsFirstConnect:
			sys.exit()
		Server_IP_Address = str(Server_IP_Address_List[Current_index])
		Current_index = (Current_index + 1) % len(Server_IP_Address_List)
		print DEBUG + 'Server IP Address: ' + Server_IP_Address
	else : 
		sys.exit()

def startSendingHEARTBEATToParentServer(TTL):
	delay = TTL / 3
	while 1:
		params = { 'username': Client_IP_Address }
		try:
			response = requests.get('http://' + Server_IP_Address + ':' + Server_Port + '/client/heartbeat', params=params )
		except:
			pass
		time.sleep(delay)

# Gets TTL from parent server
def getTTLfromParentServer():
	# TODO Implement
	return 6

# Tell parent server that you are alive and well periodically
def startHEARTBEAT():
	TTL = getTTLfromParentServer()
	
	global hb_thread
	if hb_thread and hb_thread.isAlive():
		hb_thread.stop()

	hb_thread = HEARTBEAT_Thread(1, TTL)
	try:
		hb_thread.start()
	except:
		print DEBUG + "Error: unable to start HEARTBEAT thread"

# Inform the parent server about your local video file list periodically
def startFileChecking():
	delay = 5
	fchk_thread = FILE_CHK_Thread(1, delay)
	try:
		fchk_thread.start()
	except:
		print DEBUG + "Error: unable to start FILE CHECK thread"

def startConnect():
	data = Client_IP_Address
	headers = {'Content-Type': 'text/plain'}

	video_list = getVideoList()

	if video_list:
		data = data + ',' + ','.join(video_list)

	global IsFirstConnect
	if not IsFirstConnect :
		time.sleep(10)
		getServerIPsfromMaster()
		setServerIP()
		IsFirstConnect = False

	proceed = False
	while not proceed :
		try :
			print DEBUG + 'Connecting to ' + Server_IP_Address + ':' + Server_Port	
			response = requests.post('http://' + Server_IP_Address + ':' + Server_Port + '/client/connect', headers=headers, data=data)
			if response.ok :
				proceed = True
		except :
			proceed = False
			setServerIP()


def connectToParentServer():
	# Begin connection
	startConnect()

	# After connection start heartbeat
	startHEARTBEAT()

	# After connection also start checking your file list and send the updates to the parent server
	startFileChecking()

# Extracts the proxy configuration of the client
def getProxies():
	proxies = {}
	if 'http_proxy' in os.environ:
		proxies['http_proxy'] = os.environ.get('http_proxy')
	if 'https_proxy' in os.environ:
		proxies['https_proxy'] = os.environ.get('https_proxy')
	return proxies

# Youtube search wrapper function
def searchYoutube(search_query,page):
	proxies = getProxies()
	html = requests.get(Youtube_Search_Path + urllib.quote(search_query) + '&page=' + page, proxies=proxies).text
	soup = BeautifulSoup(html)

	all_as = soup.find_all('a')
	for a in all_as:
		if 'watch' in a['href'] or 'results' in a['href']:
			a['href'] = Local_IP_Address + a['href']
		elif 'youtube' not in a['href']:
			a['href'] = Youtube_Address + a['href']

	return soup.prettify()

# Youtube watch wrapper function
def watchVideo(video_id, src = None):
	proxies = getProxies()
	html = requests.get(Youtube_Video_Address + video_id, proxies=proxies).text
	soup = BeautifulSoup(html)

	if src:
		video_divs = soup.find_all('div', {'id': 'player-mole-container'})
		for div in video_divs:
			for player_div in div.find_all('div', {'id': 'player-api'}):
				player_div.extract()
			new_tag = soup.new_tag('video', src='http://' + src + ':8000/static/Client/videos/' + video_id + '.mp4')
			new_tag['controls'] = ''
			new_tag['type'] = 'video/mp4'
			div.append(new_tag)

		move_down_divs = soup.find_all('div', {'id': 'watch7-sidebar'})
		for move_down_div in move_down_divs :
			move_down_div['style'] = 'margin-top: 0px'

	all_as = soup.find_all('a')
	for a in all_as:
		if 'watch' in a['href'] or 'results' in a['href']:
			a['href'] = Local_IP_Address + a['href']
		elif 'youtube' not in a['href']:
			a['href'] = Youtube_Address + a['href']

	return soup.prettify()

# Downloads the video using youtube-dl
def downloadFileFromYoutube(video_id):
	videos_path = Current_folder + '/static/Client/videos/'
	return_code = call(['youtube-dl', Youtube_Video_Address + video_id, '-o', videos_path + video_id + '.mp4'])
	return return_code

# Downloads the video from other client
def downloadFileFromClient(video_id,video_loc):
	print DEBUG + 'Downloading video ' + video_id + ' from ' + video_loc
	videos_path = Current_folder + '/static/Client/videos/'
	response = requests.get(video_loc, stream=True)
	
	if response.ok:
		with open(videos_path + video_id + '.mp4', 'wb') as handle:
			for block in response.iter_content(1024):
				if not block:
					break
				handle.write(block)
		return 0

	return 1


# Checks for the video in local file list
def findVideo(video_id):
	return video_id in getVideoList()

# Returns all availbale videos available locally
def getVideoList():
	videos_path = Current_folder + '/static/Client/videos/'
	video_files = [ f for f in os.listdir(videos_path) if os.path.isfile(os.path.join(videos_path,f)) ]
	video_file_names = []
	for video_file in video_files:
		match = re.search('(.+?)\.mp4', video_file)
		if match:
			video_file_names.append(match.group(1))
	return video_file_names

def check_whether_video_is_there(video_id, available):
	try:
		response = requests.get('http://' + available + ':8000/check?v=' + video_id)
		if response.ok:
			return True
	except:
		pass

	return False

# Search video in parent server
def searchVideo(video_id):
	params = {
		'username' : Client_IP_Address,
		'video' : video_id
	}

	proceed = False
	while not proceed :
		response = requests.get('http://' + Server_IP_Address + ':' + Server_Port + '/video/search', params=params)
		if response.ok :
			proceed = True
		else :
			proceed = False
			startConnect()

	# Check response

	available_list = {}
	print DEBUG + response.text
	if len(response.text) > 0:
		available_list = json.loads(response.text)

	available = None
	if available_list:
		got_it = False
		for i in xrange(len(available_list)):
			available = str(available_list[i])
			if check_whether_video_is_there(video_id, available):
				got_it = True
				break
		if not got_it:
			available = None

	if available :
		video_loc = 'http://' + available + ':8000/static/Client/videos/' + video_id + '.mp4'  
		dw_thread = DOWNLOAD_Thread(1,video_id,SERVER_VIDEO,video_loc)
		dw_thread.start()
		return available
	else :
		dw_thread = DOWNLOAD_Thread(1,video_id,YOUTUBE_VIDEO,None)
		dw_thread.start()

	return None

def initiateDelete(video_id):
	params = { 
				'username' : Client_IP_Address,
				'video' : video_id,
			}
	proceed = False
	while not proceed:
		try: 
			response = requests.delete('http://' + Server_IP_Address + ':' + Server_Port + '/video/remove', params=params )
			if response.ok :
				proceed = True
		except:
			startConnect()

	if response.text == '100':
		videos_path = Current_folder + '/static/Client/videos/'
		video_path = videos_path + video_id + '.mp4'
		os.remove(video_path)
		return True

	return False