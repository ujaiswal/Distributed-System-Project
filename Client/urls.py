from django.conf.urls import patterns, url
from Client import views
import utilities

urlpatterns = patterns('',

 	url(r'^$', views.index, name='index'),
	url(r'^results$', views.results, name='results'),
	url(r'^watch$', views.watch, name='watch'),
	url(r'^delete$', views.delete, name='delete'),
	url(r'^check$', views.check, name='check'),
	# url(r'^summary_api/$', views.summary_api, name='summary_api'),
	# url(r'^api_result/$', views.api_result, name='api_result') 
)

# Start-up code
utilities.setClientIP()
utilities.getServerIPsfromMaster()
utilities.setServerIP()
utilities.connectToParentServer()