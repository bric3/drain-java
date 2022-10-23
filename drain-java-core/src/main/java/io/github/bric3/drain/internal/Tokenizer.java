/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.bric3.drain.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Simple string tokenizer.
 */
public class Tokenizer {
    public static List<String> tokenize(String content, String delimiters) {
        StringTokenizer stringTokenizer = new StringTokenizer(content, delimiters);

        List<String> tokens = new ArrayList<>(stringTokenizer.countTokens());
        while (stringTokenizer.hasMoreTokens()) {
            String trimmedToken = stringTokenizer.nextToken().trim();
            if (!trimmedToken.isEmpty()) {
                tokens.add(trimmedToken);
            }
        }

        return tokens;
    }
}
