/*
 * Licensed to Jörg Prante and xbib under one or more contributor
 * license agreements. See the NOTICE.txt file distributed with this work
 * for additional information regarding copyright ownership.
 *
 * Copyright (C) 2016 Jörg Prante and xbib
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses
 * or write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * The interactive user interfaces in modified source and object code
 * versions of this program must display Appropriate Legal Notices,
 * as required under Section 5 of the GNU Affero General Public License.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public
 * License, these Appropriate Legal Notices must retain the display of the
 * "Powered by xbib" logo. If the display of the logo is not reasonably
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by xbib".
 */
package org.xbib.charset;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

/**
 *
 */
public class AnselCharset extends Charset {

    private static final Map<String, AnselCodeTableParser.CharacterSet> characterSetMap;

    static {
        characterSetMap = new LinkedHashMap<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = cl.getResource("org/xbib/charset/codetables.xml").openStream()) {
            AnselCodeTableParser anselCodeTableParser = new AnselCodeTableParser(inputStream);
            for (AnselCodeTableParser.CodeTable codeTable : anselCodeTableParser.getCodeTables()) {
                for (AnselCodeTableParser.CharacterSet characterSet : codeTable.getCharacterSets()) {
                    characterSetMap.put(characterSet.getName(), characterSet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Charset encodeCharset;

    public AnselCharset() throws XMLStreamException {
        super("ANSEL", BibliographicCharsetProvider.aliasesFor("ANSEL"));
        this.encodeCharset = StandardCharsets.UTF_8;
    }

    @Override
    public boolean contains(Charset charset) {
        return charset instanceof AnselCharset;
    }

    public CharsetEncoder newEncoder() {
        throw new UnsupportedOperationException();
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this, encodeCharset.newDecoder());
    }

    private static class Decoder extends CharsetDecoder {

        String g0;
        String g1;

        Decoder(Charset cs, CharsetDecoder baseDecoder) {
            super(cs, baseDecoder.averageCharsPerByte(), baseDecoder.maxCharsPerByte());
        }

        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            g0 = "Basic Latin (ASCII)";
            g1 = "Extended Latin (ANSEL)";
            CharArrayWriter w = new CharArrayWriter();
            CharArrayWriter diacritics = new CharArrayWriter();
            int pos = in.position();
            while (in.hasRemaining()) {
                byte b = in.get();
                char oldChar = (char) (b & 0xFF);
                if (oldChar == '\u001b') {
                    handleEscapeSequence(in);
                    if (in.hasRemaining()) {
                        b = in.get();
                        oldChar = (char) (b & 0xFF);
                    } else {
                        // premature end of escape sequence, no data following
                        return CoderResult.UNDERFLOW;
                    }
                }
                AnselCodeTableParser.CharacterSet characterSet = isG0(oldChar) ? characterSetMap.get(g0) :
                        isG1(oldChar) ? characterSetMap.get(g1) : null;
                int len = characterSet != null ? characterSet.getLength() : 1;
                String str = len == 1 ? "" + oldChar : "" + oldChar + (char) (in.get() & 0xFF) + (char) (in.get() & 0xFF);
                AnselCodeTableParser.Code code = characterSet != null ? characterSet.getMarc().get(str) : null;
                char ch = code != null ? code.getUcs() : oldChar;
                if (ch == '\u0000') {
                    // FB, EC - see http://memory.loc.gov/diglib/codetables/45.html#Note1 and http://memory.loc.gov/diglib/codetables/45.html#Note2
                    continue;
                }
                boolean isDiacritic = code != null ? isDiacritic(oldChar) || code.isCombining() : isDiacritic(oldChar);
                if (isDiacritic) {
                    diacritics.write(ch);
                } else {
                    w.write(ch);
                    // diacritics must be appended in Unicode, but are prepended in MARC-8 / Z39.47
                    if (diacritics.toCharArray().length > 0) {
                        try {
                            w.write(diacritics.toCharArray());
                        } catch (IOException e) {
                            // dummy
                            w.flush();
                        }
                        diacritics = new CharArrayWriter();
                    }
                }
            }
            for (char ch : w.toCharArray()) {
                if (!out.hasRemaining()) {
                    in.position(pos - 1);
                    return CoderResult.OVERFLOW;
                }
                out.put(ch);
            }
            return CoderResult.UNDERFLOW;
        }

        private boolean isDiacritic(char ch) {
            return ch >= 0xE0 && ch <= 0xFF;
        }

        private boolean isG0(char ch) {
            return ch >= 0x21 && ch <= 0x7E;
        }

        private boolean isG1(char ch) {
            return ch >= 0xA1 && ch <= 0xFE;
        }

        /**
         * ANSI X3.41 or ISO 2022 escape technique.
         * See procedures in IS0 2375-1985.
         *
         * @param in byte buffer
         */
        private void handleEscapeSequence(ByteBuffer in) {
            byte oneByte = in.get();
            switch (oneByte) {
                case 's':
                    g0 = "Basic Latin (ASCII)";
                    break;
                case 'g':
                    g0 = "Greek Symbols";
                    break;
                case 'b':
                    g0 = "Subscripts";
                    break;
                case 'p':
                    g0 = "Superscripts";
                    break;
                case '(':
                case ',':
                    oneByte = in.get();
                    switch (oneByte) {
                        case '1':
                            g0 = "Chinese, Japanese, Korean (EACC)";
                            break;
                        case '2':
                            g0 = "Basic Hebrew";
                            break;
                        case '3':
                            g0 = "Basic Arabic";
                            break;
                        case '4':
                            g0 = "Extended Arabic";
                            break;
                        case 'B':
                            g0 = "Basic Latin (ASCII)";
                            break;
                        case 'N':
                            g0 = "Basic Cyrillic";
                            break;
                        case 'Q':
                            g0 = "Extended Cyrillic";
                            break;
                        case 'S':
                            g0 = "Basic Greek";
                            break;
                        default:
                            break;
                    }
                    break;
                case ')':
                case '-':
                    oneByte = in.get();
                    switch (oneByte) {
                        case '1':
                            g1 = "Chinese, Japanese, Korean (EACC)";
                            break;
                        case '2':
                            g1 = "Basic Hebrew";
                            break;
                        case '3':
                            g1 = "Basic Arabic";
                            break;
                        case '4':
                            g1 = "Extended Arabic";
                            break;
                        case 'B':
                            g1 = "Basic Latin (ASCII)";
                            break;
                        case 'N':
                            g1 = "Basic Cyrillic";
                            break;
                        case 'Q':
                            g1 = "Extended Cyrillic";
                            break;
                        case 'S':
                            g1 = "Basic Greek";
                            break;
                        default:
                            break;
                    }
                    break;
                case '$':
                    oneByte = in.get();
                    switch (oneByte) {
                        case '1':
                            g0 = "Chinese, Japanese, Korean (EACC)";
                            break;
                        default:
                            break;
                    }
                    break;
                case '!':
                    oneByte = in.get();
                    switch (oneByte) {
                        case 'E':
                            g0 = "Extended Latin (ANSEL)";
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

}


