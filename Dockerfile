FROM ubuntu:20.04

ENV AGENT_VERSION $AGENT_VERSION

RUN mkdir -p /data-download/agent
RUN mkdir -p /data/agent

COPY ./build/install/linuxX64/* /data-download/agent/
COPY ./build/install/linuxX64/* /data/agent/
COPY commands.sh /commands.sh
RUN ["chmod", "+x", "/commands.sh"]

# Run the command on container startup
CMD ["/commands.sh"]
