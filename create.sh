#!/bin/sh

rm -rf classes
mkdir -p classes
mkdir -p dist
mkdir -p archive

if [ ! -e "archive/groovy-all-1.7.4.jar" ]
then
if [ -e "/usr/share/java/groovy-all-1.7.4.jar" ] 
then
cp /usr/share/java/groovy-all-1.7.4.jar archive/groovy-all-1.7.4.jar
else
wget -U duplo http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-all/1.7.4/groovy-all-1.7.4.jar -O archive/groovy-all-1.7.4.jar
fi
fi

cp archive/groovy-all-1.7.4.jar dist/duplo.jar
cd classes
groovyc ../src/duplo.groovy
jar uvf ../dist/duplo.jar .
cd ..
jar umf MANIFEST.MF dist/duplo.jar
