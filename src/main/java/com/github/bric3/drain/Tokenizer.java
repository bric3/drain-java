/*
 * drain-java
 *
 * Copyright (c) 2021, Today - Brice Dutheil
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.github.bric3.drain;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

class Tokenizer {
    public static List<String> tokenize(String content, String delimiters) {
        var stringTokenizer = new StringTokenizer(content, delimiters);

        var tokens = new ArrayList<String>(stringTokenizer.countTokens());
        while (stringTokenizer.hasMoreTokens()) {
            var trimmedToken = stringTokenizer.nextToken();
            if (!trimmedToken.isBlank()) {
                tokens.add(trimmedToken.trim());
            }
        }

        return tokens;
    }
}
