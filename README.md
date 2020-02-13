# Drill agent module

This module contains the native agent used to profile a java application and report 
the results to Drill4J admin services.

Agent binaries are being built to build/install/{target platform identifier}.

To run the demo application (Petclinic) with a built agent, ./gradlew run command 
shall be used. Thus the Petclinic application will be started on port 8080, and
agent will be configured to connect to the admin back-end on port 8090.

The agent is being republished whenever changes are applied to the 'develop' branch.
