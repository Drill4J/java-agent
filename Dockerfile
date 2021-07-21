FROM bash:5.0.18

RUN mkdir -p /data-download/agent
RUN mkdir /data

COPY ./build/install/linuxX64/* /data-download/agent/
COPY commands.sh /commands.sh
RUN ["chmod", "+x", "/commands.sh"]

# Run the command on container startup
ENTRYPOINT ["/commands.sh"]
