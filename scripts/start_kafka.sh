#!/bin/bash

# start_kafka.sh ----------------------------------------------------------------------------------
#
# Script Description:
#     This script will run start Zookeeper and Kafka. Also creates the 'orders' topic in Kafka. 
#
# Comments:
#     This script should be placed in the main kafka directory!
#     The script uses a while loop after each command, to ensure that processes have started before
#     continuing. The grep command checks for the configuration file that was passed when starting
#     the services and can be changed accordingly.  
# -------------------------------------------------------------------------------------------------


## For AWS micro instance with limited memory
export KAFKA_HEAP_OPTS="-Xmx256M -Xms128M"

# Start Zookeeper
nohup bin/zookeeper-server-start.sh -daemon config/zookeeper.properties > /dev/null 2>&1 &
while true; do 
   if (( $(ps -ef | grep "config/zookeeper.properties" | grep -v grep | wc -l) == 1 )); then
      echo "Zookeeper process has started"
      sleep 5 # just to be sure that is fully loaded
      break;
   fi
done


# Start Kafka
nohup bin/kafka-server-start.sh -daemon config/server.properties > /dev/null 2>&1 &
while true; do 
   if (( $(ps -ef | grep "config/server.properties" | grep -v grep | wc -l) == 1 )); then
      echo "Kafka process has started"
      sleep 5 # just to be sure that is fully loaded
      break;
   fi
done

# Create the 'orders' topic where the data will be pushed
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 2 --topic orders

