# Echo

This project contains tracing for applications based on Play, Akka and Scala Futures.  
It uses load time weaving and [AspectJ](http://eclipse.org/aspectj/) to do so.  
The project is a subset of what constitutes the Typesafe Console and is used in [Typesafe Activator](http://wwww.typesafe.com/activator) under the `Inspect` tab.

## Running

To run the project you should

	> sbt
	> project collector
	> run

  
or simply,

	>sbt collector/run
  
This will start a collector that listens to `trace events` emitted from a traced application.

Please note that by just running this project you will not achieve much as it is intended to be used in collaboration with [Elucidator](https://github.com/typesafehub/elucidator).

## Tracing of Applications

If you want to trace an application you must make sure that the AOP files (*.aj) are availble to the load time weaver. 

There are a set of defined AOP files in the project that are based on what version of Play, Akka or Scala. You can find these files under the `trace`folder in the project.

## Tracing Example

Say you want to trace an Akka 2.2 + Scala 2.10 based application. The absolutely simplest way to go about is to use the sbt-echo plugin. It is also possible to run everything manually should you need/want to. The two sections below describe the two alternatives.

#### Running from sbt		
		
There is a convenient sbt plugin for this named [sbt-echo](https://github.com/typesafehub/sbt-echo). See the project for more information of how to get started.

#### Running manually 

There will be a published version availble that you should refer to in your sbt project, e.g. 

	"com.typesafe.trace" %% "trace-akka-2.2.1" % "0.1"

Replace `0.1` with whatever version it is you want to use.

It is also possible to publish a version locally should you like to add things and experiment with the code, i.e.,

	> sbt
	> project trace-akka22-scala210
	> publish-local
	
This publishes the JAR file into your local repository which can be found here `~/.ivy2/local`.   
	
Thereafter you must also instruct the tracer what to trace in an `application.conf` file in your project, e.g.

	activator {
		trace {
    		enabled = true
    		node = AwesomeApplication
 
    		traceable {
      			"*" = on     
    		}
 
    		sampling {
      			"*" = 1   
    		}
  		}		
	 
		collect {
    		# Remote setting - where the remote collector resides
    		remote {
      			hostname = "127.0.0.1"
      			port = 2553
    	}
  	}

The `application.conf` file should be put `src/main/resources`.

The next step is to ensure that AspectJ is used when you run your application. This is done by adding a `javaagent` to the start command of your application, e.g.   

	>java -javaagent:<your_lib>/aspectjweaver.jar

Of course you have to replace <your_lib> above with the actual path to the aspectweaver.jar file.

#### Standalone JAR

You can create a JAR file out of your sbt project with the plugin [sbt-onejar](https://github.com/sbt/sbt-onejar).
   	
Now you should be good to go. Add your project to the Java command above, i.e.

	>java -jar -javaagent:<your_lib>/aspectjaweaver.jar YourAwesomeApp.jar
		
#### Troubleshooting	

If you haven't started the collector process (see Running section above) you will see the following warning when you run your traced application: `WARN: No trace receiver on port [28667] (retry enabled)`.
