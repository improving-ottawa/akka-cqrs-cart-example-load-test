#!/bin/bash
USER_ARGS="-Dsomething=$1"
#//COMPILATION_CLASSPATH=`find -L ./target -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`
COMPILATION_CLASSPATH="/Users/dkichler/.ivy2/local/com.lightbend.akka.samples/shopping-cart-load-test_2.13/0.1.0-SNAPSHOT/jars/shopping-cart-load-test_2.13-tests.jar"
JAVA_OPTS="-server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx2048M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"
java $JAVA_OPTS $USER_ARGS -cp $COMPILATION_CLASSPATH io.gatling.app.Gatling -s com.lightbend.akka.samples.load.ShoppingCartServiceLoadTest
