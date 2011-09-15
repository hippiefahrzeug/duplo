#!/bin/sh

rm -rf classes
mkdir -p classes
mkdir -p dist
cp /usr/share/java/groovy-all-1.7.4.jar dist/duplicatefinder.jar
cd classes
groovyc ../src/duplicatefinder.groovy
jar uvf ../dist/duplicatefinder.jar .
cd ..
jar umf MANIFEST.MF dist/duplicatefinder.jar
