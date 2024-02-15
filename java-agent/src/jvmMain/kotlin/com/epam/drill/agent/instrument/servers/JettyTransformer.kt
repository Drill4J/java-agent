package com.epam.drill.agent.instrument.servers

import com.epam.drill.agent.instrument.DefaultHeadersProcessor
import com.epam.drill.agent.instrument.HeadersProcessor
import com.epam.drill.agent.instrument.TransformerObject
import com.epam.drill.agent.request.HeadersRetriever

actual object JettyTransformer :
    TransformerObject,
    JettyTransformerObject(HeadersRetriever),
    HeadersProcessor by DefaultHeadersProcessor
