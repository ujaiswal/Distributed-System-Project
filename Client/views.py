from django.shortcuts import render
from django.http import HttpResponseRedirect, HttpResponse
from django.core.urlresolvers import reverse
import utilities

# Create your views here.
def index(request):
	return render(request, 'Client/index.html', {})

def results(request):
	if 'search_query' not in request.GET or len(request.GET['search_query']) < 1 :
		return render(request, 'Client/index.html', { 'error_message': 'Let us know what you want to search for'})

	search_string = request.GET['search_query']

	page = '1'
	if 'page' in request.GET:
		page = request.GET['page']

	result = utilities.searchYoutube(search_string, page)
	return HttpResponse(result)

def watch(request):
	if 'v' not in request.GET or len(request.GET['v']) < 1:
		return render(request, 'Client/index.html', { 'error_message': 'Let us know what you want to search for'})

	video_id = request.GET['v']

	result = 'We will get you ' + video_id + '.mp4 soon.'
	src = None
	if utilities.findVideo(video_id) :
		src = 'localhost'

	return HttpResponse(utilities.watchVideo(video_id,src))