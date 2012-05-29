echo "Building Activiti "
cd activiti-kickstart-java
mvn clean install -DskipTests
cd ..
cd activiti-kickstart-ui
mvn -DKickStartDebugInMem=true clean package jetty:run 