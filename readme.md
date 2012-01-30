Install: 
mvn install:install-file -Dfile=exe-webapp-maven-archetype-1.0.jar -DgroupId=com.strumsoft.maven -DartifactId=exe-webapp-maven-archetype -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true

Run:
mvn archetype:create -DarchetypeGroupId=com.strumsoft.maven -DarchetypeArtifactId=exe-webapp-maven-archetype -DarchetypeVersion=1.0 -DgroupId=your.group.id -DartifactId=your-project-name