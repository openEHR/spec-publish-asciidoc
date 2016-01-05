#!/bin/bash

if [ -z "$MAGICDRAW_HOME" ]; then
    echo "MAGICDRAW_HOME environment variable not set, please set it to the MagicDraw installation folder"
    exit 1
fi

CP=$MAGICDRAW_HOME/lib/com.nomagic.osgi.launcher-17.0.5-SNAPSHOT.jar:$MAGICDRAW_HOME/lib/bundles/org.eclipse.osgi_3.10.1.v20140909-1633.jar:$MAGICDRAW_HOME/lib/bundles/com.nomagic.magicdraw.osgi.fragment_1.0.0.201512211944.jar:$MAGICDRAW_HOME/lib/md_api.jar:$MAGICDRAW_HOME/lib/md_common_api.jar:$MAGICDRAW_HOME/lib/md.jar:$MAGICDRAW_HOME/lib/md_common.jar:$MAGICDRAW_HOME/lib/jna.jar
java -Xmx1200M -Xss1024K \
       -Dmd.class.path="file:$MAGICDRAW_HOME/bin/magicdraw.properties?base=$MAGICDRAW_HOME#CLASSPATH" \
       -Dcom.nomagic.osgi.config.dir=$MAGICDRAW_HOME/configuration \
       -Desi.system.config=$MAGICDRAW_HOME/data/application.conf \
       -Dlogback.configurationFile=$MAGICDRAW_HOME/data/logback.xml \
       -Dcom.nomagic.magicdraw.launcher=org.openehr.docs.magicdraw.AsciidocCommandLine  \
       -cp $CP \
       -Dmd.additional.class.path=$MAGICDRAW_HOME/plugins/org.openehr.docs.magicdraw/OpenEhrModelExporter.jar  \
       com.nomagic.osgi.launcher.ProductionFrameworkLauncher $@
