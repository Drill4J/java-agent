FROM ubuntu:20.04

RUN apt-get update && apt-get install -y \
  cron \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /data-download/agent
RUN mkdir /data
COPY check-file-exist.sh /
RUN chmod +x /check-file-exist.sh
COPY ./build/install/linuxX64/* /data-download/agent/

# Copy copy-files-cron file to the cron.d directory
COPY copy-files-cron /etc/cron.d/copy-files-cron
 
# Give execution rights on the cron job
RUN chmod 0644 /etc/cron.d/copy-files-cron

# Apply cron job
RUN crontab /etc/cron.d/copy-files-cron
 
# Create the log file to be able to run tail
RUN touch /var/log/cron.log
 
# Run the command on container startup
CMD cron && tail -f /var/log/cron.log
