from django.conf.urls import include, url
from django.contrib import admin

urlpatterns = [
    # Examples:
    # url(r'^$', 'ReplicatedSystems.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),
    url(r'^', include('Client.urls', namespace='Client')),
    url(r'^admin/', include(admin.site.urls)),
]
