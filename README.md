# akka-simple-example
Simple examples


# Runnig Visualizer (akka-visualmailbox) for this project

## How to install akka-visualmailbox
Instala sbt - see http://www.scala-sbt.org/download.html


Install npm + bower
```
// install npm 
...
sudo apt-get install nodejs
pacman -S nodejs npm
```

```
sudo ln -s /usr/bin/nodejs /usr/bin/node
```
or install legacy nodejs:
```
sudo apt-get install nodejs-legacy
```

## Clone visualizer project - see https://github.com/ouven/akka-visualmailbox
https://github.com/ouven/akka-visualmailbox.git


## Run visualizer
```
cd akka-visualmailbox
bower install
sbt "project visualization" run
```

## Add maven dependency in this akka project (its already in the pom.xml):
```
<repository>
   <id>osssonatype</id>
   <url>https://oss.sonatype.org/content/repositories/releases</url>
</repository>
...
<dependency>
   <groupId>de.aktey.akka.visualmailbox</groupId>
   <artifactId>collector_2.11</artifactId>
   <version>1.0.0</version>
</dependency>
```

## Add config in your project (already in application.conf)
```
akka {
 actor {
   default-mailbox.mailbox-type = "de.aktey.akka.visualmailbox.VisualMailboxType"
 }
}
```


## Run some long-running example in this project (akka-simple-example). 


## Open webbrowser
http://localhost:8080


## Current state:
Not possible to compile/run visualizer itself
```
bower --version
1.7.9

npm --version
1.3.10


sbt --version
sbt launcher version 0.13.11 (??)
```
