By:
===
David Miller
JT Barrett
Josh Melton

Setup:
======
Open "Application Server" directory. 
Create a new directory "bin".  
Copy the folder config into bin. 
Alternatively, in the future, hunt down all the places that need relative files paths changed to make it work without doing this.  

Compile:
========
From "Application Server" directory,
$ javac -d bin -cp bin -s src src/**/*.java src/appserver/**/*.java
Now enter the bin directory.

Start Simple Web Server:
========================
java web.SimpleWebServer

Start Satellite:
================
java appserver.satellite.Satellite 

Start Client
============
java appserver.client.PlusOneClient

To Do Next time:
================
Complete code stubs in appserver/server/Server.java - see LoadManager and SatelliteManager.
Write one of our own Satellites? 

Don't Touch (these files are not meant to be changed)
====================
PlusOne
Client

