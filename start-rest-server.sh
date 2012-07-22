cd activiti-kickstart-java
mvn -DskipTests clean install 
cd ..
cd activiti-kickstart-rest
export MAVEN_OPTS="-Xms521M -Xmx1024M -noverify -javaagent:/Applications/ZeroTurnaround/JRebel/jrebel.jar -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
mvn -D jetty.port=9000 clean package jetty:run