FROM ubuntu:20.04

ENV AGENT_VERSION $AGENT_VERSION

RUN apt-get update && apt-get install -y \
  wget \
  unzip \
  curl \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /data-download/agent
RUN mkdir /data
# RUN mkdir /data/agent

COPY ./build/install/linuxX64/* /data-download/agent/
# COPY ./build/install/linuxX64/* /data/agent/
COPY commands.sh /commands.sh
COPY download-artifact.sh /download-artifact.sh

# Run the command on container startup
CMD ["/commands.sh"]
