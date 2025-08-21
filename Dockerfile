FROM alpine:3.20

ARG DRILL_AGENT_VERSION

RUN if [ -z "$DRILL_AGENT_VERSION" ]; then \
      echo "ERROR: DRILL_AGENT_VERSION build arg not provided" && exit 1; \
    fi \
 && apk add --no-cache unzip wget \
 && wget -O /tmp/drill-agent.zip https://github.com/Drill4J/java-agent/releases/download/v${DRILL_AGENT_VERSION}/agent-linuxX64-${DRILL_AGENT_VERSION}.zip \
 && unzip -o /tmp/drill-agent.zip -d /opt \
 && mv /opt/linuxX64-${DRILL_AGENT_VERSION} /opt/drill-agent \
 && rm /tmp/drill-agent.zip

VOLUME ["/opt/drill-agent"]