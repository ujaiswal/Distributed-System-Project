# Replicated Systems  
[![Build Status](https://travis-ci.org/ujaiswal/ReplicatedSystems.svg)](https://travis-ci.org/ujaiswal/ReplicatedSystems)  
Distributed Systems Term Project
## Team
* Utkarsh Jaiswal  
* Shushman Choudhury  
* Ankit Jain  
* Aniruddha Gupta  


initial configration:

sudo apt-get install python-pip
sudo pip install -r requirements.txt
sudo apt-get install openjdk-7-jdk 

before running. 
Change IP address in /ParenServer/src/main/java/hello/Server.java line 63 to IP of master server.
Also on line 19	of Client/utilities.py 


To run Master server:
cd MasterServer
./gradlew build
./gradlew run


To run Parent server:
cd ParentServer
./gradlew build
./gradlew run

To run client
python manage.py runserver 0.0.0.0:8000


finally open localhost:8000 in browser