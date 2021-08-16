#!/usr/bin/env sh
java -Djava.util.concurrent.ForkJoinPool.common.parallelism=128 -jar api.jar "$@" 2>&1
