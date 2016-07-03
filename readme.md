#glassdoor [![Build Status](https://travis-ci.org/fschrofner/glassdoor.svg?branch=master)](https://travis-ci.org/fschrofner/glassdoor)
*Please note that this framework is still work in progress and might not be usable at all at the moment!*  

glassdoor is a modern, autonomous security framework for Android APKs written in [Scala](http://scala-lang.org/).  
Its purpose is to automatically find backdoors, security flaws and other data leakages in applications running on the Android system, without having any actual access to the code itself.

##Dependencies
###Build Dependencies
Scala

###Runtime Dependencies
Java  
Grep  
Git 

##Build Instructions
Glassdoor uses the wonderful [Gradle](https://gradle.org/) build system to realise its builds.  
In order to build glassdoor yourself, it should be sufficient to open a terminal window in the root directory and call `./gradlew build` there. You will find a ready-to-use version of glassdoor, compressed as tar and zip file, inside `build/distributions` after the build process. Just take either one, extract it and call `bin/glassdoor`.  
If you want to run glassdoor directly (for development purposes, for example) just call `./gradlew run` in the root directory.  
Here you go, your self-compiled version of glassdoor is up and running (hopefully)!

###IntelliJ IDEA
To start hacking and slashing on glassdoor yourself using the terrific [IntelliJ IDEA](https://www.jetbrains.com/idea/), you need to take further steps.  
First you should install the Scala plugin for IntelliJ. After that you can generate the project files needed by issuing `./gradlew idea` in the root directory.
The next step is to import the project into IntelliJ, by selecting "File" > "Open". Just select the outermost folder (yeah you want all modules to be checked).
It should load the project correctly; now to simplify development you should setup the run configuration. To do so, select "Edit Configurations" next to the run button.
Add a new Gradle configuration, select the outermost glassdoor Gradle project and use the task "run" (without quotes). Give your configuration a name and click "OK".
Then select your newly created configuration from the dropdown menu next to the run button. You should now be able to run and debug glassdoor using IntelliJ.

##Libraries
Credit, where credit is due. Glassdoor makes extensive use of libraries, see the following list to find out which libraries were used.

[JLine2](https://github.com/jline/jline2) - [BSD](https://opensource.org/licenses/bsd-license)  
[Smali](https://github.com/JesusFreke/smali) - [Custom Licence](https://github.com/JesusFreke/smali/blob/master/NOTICE)  
[Config](https://github.com/typesafehub/config) - [Apache 2.0](https://opensource.org/licenses/Apache-2.0)  
[Akka](https://github.com/akka/akka) - [Apache 2.0](https://opensource.org/licenses/Apache-2.0)  
[Scala-Logging](https://github.com/typesafehub/scala-logging) - [Apache 2.0](https://opensource.org/licenses/Apache-2.0)  
[Logback](https://github.com/qos-ch/logback) - [EPL 1.0](https://opensource.org/licenses/EPL-1.0)  
[Spoon](https://github.com/INRIA/spoon) - [CECILL](https://opensource.org/licenses/CECILL-2.1)  

##Licence
//TODO
