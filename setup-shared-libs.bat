rmdir /s /q lib-jvm-shared
git clone https://github.com/Drill4J/lib-jvm-shared lib-jvm-shared --branch alpine-support/no-http-hook
gradlew :updateSharedLibs
