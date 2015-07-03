#!/bin/bash

if [ -z "$MAGICDRAW_HOME" ]; then
    echo "MAGICDRAW_HOME environment variable not set, please set it to the MagicDraw installation folder"
    exit 1
fi

CP=$MAGICDRAW_HOME/plugins/org.openehr.docs.magicdraw/OpenEhrModelExporter.jar
for i in `find $MAGICDRAW_HOME/lib -name \*jar`; do
    CP=$CP:$i
done

java -cp $CP -Dinstall.root=$MAGICDRAW_HOME -Xmx1200M -Xss1024K org.openehr.docs.magicdraw.AsciidocCommandLine $@