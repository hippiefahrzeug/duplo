#!/bin/sh

rm -rf classes
mkdir -p classes
mkdir -p dist
cp /usr/share/java/groovy-all-1.7.4.jar dist/duplo.jar
cd classes
groovyc ../src/duplo.groovy
jar uvf ../dist/duplo.jar .
cd ..
jar umf MANIFEST.MF dist/duplo.jar
