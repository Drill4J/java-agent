package com.epam.drill.agent.proxy;

import com.alibaba.ttl.threadpool.agent.TtlAgent;

import java.lang.instrument.Instrumentation;

class ProxyAgent {
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        TtlAgent.premain(agentArgs, instrumentation);
        System.out.println(">>>Ttl agent attached.");
    }
}
