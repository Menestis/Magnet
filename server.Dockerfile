FROM itzg/minecraft-server:java8

ENV EULA=TRUE \
    TYPE=CUSTOM \
    CUSTOM_SERVER=/server.jar \
    VERSION=1.8.8 \
    INIT_MEMORY=512M \
    MAX_MEMORY=4G \
    ONLINE_MODE=FALSE \
    MAX_PLAYERS=100 \
    ALLOW_FLIGHT=TRUE \
    ANNOUNCE_PLAYER_ACHIEVEMENTS=false \
    SPAWN_PROTECTION=0 \
    COPY_CONFIG_DEST=/data/ \
    USE_AIKAR_FLAGS=true

COPY assets/spigot/server.jar /
#COPY assets/spigot/spigot.yml /config/
COPY assets/spigot/sportpaper.yml /config/
COPY target/magnet-0.1.jar /plugins/
COPY assets/spigot/Vulcan-2.7.0.jar /plugins/
COPY assets/spigot/ViaVersion-4.3.1.jar /plugins/
COPY assets/spigot/Vulcan/ /plugins/Vulcan/
