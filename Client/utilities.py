import os
import socket
import requests
import urllib
from bs4 import BeautifulSoup
from subprocess import call
import re
import threading
import time

DEBUG = '[R-Youtube] '

# Required to set manually
Master_Server_IP_Address = '0.0.0.0'

Client_IP_Address = '0.0.0.0'
Server_IP_Address = '0.0.0.0'

Local_IP_Address = 'http://localhost:8000'
Youtube_Address = 'https://youtube.com'

Youtube_Video_Address = 'https://www.youtube.com/watch?v='
Youtube_Search_Path = 'https://www.youtube.com/results?search_query='
Current_folder = os.path.abspath(os.path.dirname(__file__))

class HEARTBEAT_Thread (threading.Thread):
	def __init__(self, threadID, TTL):
		threading.Thread.__init__(self)
		self.threadID = threadID
		self.TTL = TTL

	def run(self):
		startSendingHEARTBEATToParentServer(self.TTL)

class FILE_CHK_Thread (threading.Thread):
    def __init__(self, threadID, delay):
        threading.Thread.__init__(self)
        self.threadID = threadID
        self.delay = delay

    def run(self):
    	video_list = getVideoList()
    	# TODO Send this video list to the parent server

    	prev_video_list = set(video_list)
    	while 1:
    		time.sleep(self.delay)
    		curr_video_list = set(getVideoList())
    		
    		# TODO Compare the prev video list and current video list and send the add or delete command to the server
    		addititons = curr_video_list.difference(prev_video_list)
    		if len(addititons) > 0 :
    			print DEBUG + 'Sending addition to ' + Server_IP_Address

    		deletions = prev_video_list.difference(curr_video_list)
    		if len(deletions) > 0 :
    			print DEBUG + 'Sending deletion to ' + Server_IP_Address

    		prev_video_list = curr_video_list

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
def  getServerIPfromMaster():
	# TODO: Implement
	return '0.0.0.0'

# Sets the global variable Server_IP_Address
def setServerIP():
	global Server_IP_Address
	Server_IP_Address = getServerIPfromMaster()
	print DEBUG + 'Server IP Address: ' + Server_IP_Address

def startSendingHEARTBEATToParentServer(TTL):
	delay = TTL / 3
	while 1:
		# TODO Implement heartbeat sending code

		print DEBUG + '%s:\t%s' % ( 'Sent HEARTBEAT to ' + Server_IP_Address + ' at', time.ctime(time.time()) )
		time.sleep(delay)

# Gets TTL from parent server
def getTTLfromParentServer():
	# TODO Implement
	return 6

# Tell parent server that you are alive and well periodically
def startHEARTBEAT():
	TTL = getTTLfromParentServer()
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

def connectToParentServer():
	# TODO Implement connection code

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
			# div['src'] = 'http://' + src + ':8000/static/Client/videos/' + video_id + '.mp4'
			new_tag['controls'] = ''
			new_tag['type'] = 'video/mp4'
			div.append(new_tag)

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

# Checks for the video in local file list
def findVideo(video_id):
	return video_id in getVideoList()

# Returns all availbale videos available locally
def getVideoList():
	videos_path = Current_folder + '/static/Client/videos/'
	video_files = [ f for f in os.listdir(videos_path) if os.path.isfile(os.path.join(videos_path,f)) ]
	video_file_names = [re.search('(.+?)\..+', video_file).group(1) for video_file in video_files ]
	return video_file_names
