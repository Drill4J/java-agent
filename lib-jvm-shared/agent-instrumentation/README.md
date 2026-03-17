# http-clients-instrumentation

This repository contains common instrumentation for http clients such as
<li>Apache Client</li>
<li>OkHttpClient</li>
<li>JavaHttpClient</li>

## What does instrumentation do

Drill headers are added for every request sent using one of the http clients. <br>
Also when receiving http requests, we save the drill headers. 

## Where library is using
- [Java Agent](https://github.com/Drill4J/java-agent)
- [Autotest Agent](https://github.com/Drill4J/autotest-agent)
