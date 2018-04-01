#!/bin/bash
nohup java -jar -Dspring.profiles.active=prod /home/serdaroquai/github/yaps-server/target/MinerWatch-0.1.0.jar >> /dev/null 2>&1 &