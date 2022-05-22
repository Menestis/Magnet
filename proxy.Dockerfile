FROM itzg/bungeecord

ENV TYPE=VELOCITY VELOCITY_VERSION=3.1.1 VELOCITY_BUILD_ID=98\
 INIT_MEMORY=512m MAX_MEMORY=2G \
 NETWORKADDRESS_CACHE_TTL=10

COPY assets/velocity/server-icon.png /config/
COPY assets/velocity/velocity.toml /config/

COPY target/magnet-0.1.jar /plugins/
