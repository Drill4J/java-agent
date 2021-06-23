# Drill agent module

This module contains the native agent used to profile a java application and report 
the results to Drill4J admin services.

The agent is being republished whenever changes are applied to the 'develop' branch.

## Local development

### Run demo
To run the demo application (Petclinic) with a built agent,

    ./gradlew run 
command shall be used.

Thus the Petclinic application will be started on port 8080, and
agent will be configured to connect to the admin back-end on port 8090.


### Build assets 
Agent binaries are being built to 'build/install/{target platform identifier}'.

Where {target platform identifier}:
- mingwX64
- linuxX64

For example, for Linux it would be in 'build/install/linuxX64'

To create these files need to invoke Gradle task 'install{target platform identifier}Dist'. Example for linux:

    ./gradlew installLinuxX64Dist
For Linux ensure that 

    apt-get install libncurses5

#### Publish in Maven local

'publish{target platform identifier}ZipPublicationToMavenLocal'

Example:

    publishMingwX64ZipPublicationToMavenLocal

