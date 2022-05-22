#!/bin/bash
docker build -t registry.aspaku.com/skynet/server -f server.Dockerfile . && docker push registry.aspaku.com/skynet/server
docker build -t registry.aspaku.com/skynet/proxy -f proxy.Dockerfile . && docker push registry.aspaku.com/skynet/proxy