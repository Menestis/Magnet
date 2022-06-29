#!/bin/bash
docker build -t registry.aspaku.com/skynet/server -f server.Dockerfile .
docker build -t registry.aspaku.com/skynet/proxy -f proxy.Dockerfile .