/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.chat.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

final class CursorStore {

    private static final String CURSOR_FILE_NAME = "cursor.json";

    private final Path cursorFile;
    private final ObjectMapper objectMapper;

    CursorStore(Path stateDir) {
        this.cursorFile = stateDir.resolve(CURSOR_FILE_NAME);
        this.objectMapper = new ObjectMapper();
    }

    Optional<CursorState> load() throws IOException {
        if (!Files.exists(cursorFile)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(cursorFile.toFile(), CursorState.class));
    }

    void save(CursorState state) throws IOException {
        Files.createDirectories(cursorFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(cursorFile.toFile(), state);
    }

    record CursorState(Map<String, String> channels) {
        static CursorState empty() {
            return new CursorState(Map.of());
        }
    }
}
