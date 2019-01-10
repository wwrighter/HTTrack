# HTTrack
HTTrack; Java; HTTP v1.0; Clone; Simple Web Server

This repository is meant to serve as a basic Java program that demonstrates HTTP version 1.0.  The program is segmented into two parts.  (1) Cloning a local copy of a website by traversing all relevant links and creating a local copy of the documents in addition to the directory structure, and (2) creating a multi-threaded web server with basic functionality capable of serving GET requests to expose the mirrored website to the internet.  

Several improvements could be made on the following code base, but hopefully this helps serve as an introduction to the HTTP v1.0 structure and website traversal/cloning. 

# Basic Synopsis of HTTrack:
Given a URL, HTTrack downloads the corresponding HTML document, parses it, and downloads all documents reference in the initial HTML document on the same server.  A mirror copy of both the directory structure and the corresponding documents from the same initial server are stored locally.  The parsing and traversal performed by HTTrack assume a basic HTML link structure/syntax (i.e. <a href ="[http://host_name/]path_name>some text</a>).

# Basic Overview of WebServer
Given a Port #, WebServer listens on the specified port for basic HTTP GET requests.  Web server is setup to serve the locally mirrored version of the website copied via HTTrack and will serve requests that exist otherwise yields either a 404 or 400 response.  

Please see the included README for further information. 
