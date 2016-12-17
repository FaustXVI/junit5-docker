#!/usr/bin/env awk -f
BEGIN { 
    TRUE=0
    FALSE=1
    JAVA_STARTED=FALSE; 
}
{
    if(match($0,"^#")){
    } else if(match($0,"^Feature:(.*)$")) {
        print "---";
        print "layout: page";
        print gensub(/^Feature:(.*)$/, "title:\\1", "g", $0);
        print "category: doc";
        print "order: 0";
        print "---";
    } else if(match($0,"^\"\"\"$")) {
        if(JAVA_STARTED) {
            print "```java";
            JAVA_STARTED=TRUE
        } else {
           print "```";
           JAVA_STARTED=FALSE
        }
    } else if(match($0,"^ *Scenario:(.*)$")) {
        print gensub(/^ *Scenario:(.*)$/, "###\\1", "g", $0);
    } else {
        print $0;
    }
}
