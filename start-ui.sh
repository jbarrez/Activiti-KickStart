echo "Building Activiti "
cd activiti-kickstart-java
mvn clean install -DskipTests
cd ..
cd activiti-kickstart-ui
export MAVEN_OPTS="-Xms521M -Xmx1024M -noverify -javaagent:/Applications/ZeroTurnaround/JRebel/jrebel.jar -Xdebug -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
mvn -DKickStartDebugInMem=true clean package jetty:run 