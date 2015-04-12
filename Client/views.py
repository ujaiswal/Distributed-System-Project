from django.shortcuts import render
from django.http import HttpResponseRedirect, HttpResponse, Http404
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

	src = None
	if utilities.findVideo(video_id) :
		src = 'localhost'
	else :
		src = utilities.searchVideo(video_id)

	return HttpResponse(utilities.watchVideo(video_id,src))

def delete(request):
	if 'remove' in request.POST and len(request.POST['remove']) > 0 :
		# TODO Delete the video
		video_id = request.POST['remove']
		return render(request, 'Client/index.html', { 'error_message': 'Delete for video ' + video_id + ' initiated'})

	video_list = utilities.getVideoList()
	return render(request, 'Client/delete.html', {'video_list': video_list})

def check(request):
	if 'v' not in request.GET or len(request.GET['v']) < 0:
		raise Http404('Video id not present')

	video_id = request.GET['v']
	if utilities.findVideo(video_id) :
		return HttpResponse('It\'s there')
	else :
		raise Http404('Video is not present')
