/**
 * Copyright 2020 - 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.drill.agent.ttl.spi;

import com.epam.drill.agent.ttl.TtlCallable;
import com.epam.drill.agent.ttl.TtlRunnable;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link TtlAttachments} delegate/implementation.
 *
 * @author Jerry Lee (oldratlee at gmail dot com)
 * @see TtlRunnable
 * @see TtlCallable
 * @since 2.11.0
 */
public class TtlAttachmentsDelegate implements TtlAttachments {
    private final ConcurrentMap<String, Object> attachments = new ConcurrentHashMap<>();

    @Override
    public void setTtlAttachment(@NonNull String key, Object value) {
        attachments.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getTtlAttachment(@NonNull String key) {
        return (T) attachments.get(key);
    }
}
