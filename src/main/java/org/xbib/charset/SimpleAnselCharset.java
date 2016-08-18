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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a simplified version of "ANSEL charset" at http://anselcharset.sourceforge.net/
 * by Piotr Andzel.
 * Original code licensed under LGPL http://www.gnu.org/licenses/lgpl.html
 */
public class SimpleAnselCharset extends Charset {

    private final Map<Character, byte[]> mapping;
    private final Map<Byte, ReverseMappingEntity> reverseMapping;

    public SimpleAnselCharset() {
        super("SIMPLE_ANSEL", BibliographicCharsetProvider.aliasesFor("SIMPLE_ANSEL"));
        mapping = createMapping(getClass().getResourceAsStream("ansel-mapping.txt"));
        reverseMapping = createReverseMapping(mapping);
    }

    private static Map<Character, byte[]> createMapping(InputStream mappingStream) {
        Map<Character, byte[]> mapping = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(mappingStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int i = line.indexOf(";");
                if (i < 0) {
                    i = line.indexOf("#");
                }
                if (i >= 0) {
                    line = line.substring(0, i);
                }
                String[] kvp = line.split("=");
                if (kvp.length == 2) {
                    String uni = kvp[0];
                    String ans = kvp[1];
                    Character uniCode = (char) Integer.parseInt(uni.replaceFirst("^[uU]", ""), 16);
                    String[] ansSeq = ans.split(" ");
                    byte[] ansCodes = new byte[ansSeq.length];
                    for (int j = 0; j < ansSeq.length; j++) {
                        ansCodes[j] = (byte) (Integer.parseInt(ansSeq[j].replaceFirst("^0[xX]", ""), 16) & 0xFF);
                    }
                    mapping.put(uniCode, ansCodes);
                }
            }
        } catch (IOException e) {
            Logger.getLogger(SimpleAnselCharset.class.getName()).log(Level.WARNING, e.getMessage(), e);
        }
        return mapping;
    }

    private static Map<Byte, ReverseMappingEntity> createReverseMapping(Map<Character, byte[]> mapping) {
        Map<Byte, ReverseMappingEntity> rev = new TreeMap<>();
        for (Map.Entry<Character, byte[]> e : mapping.entrySet()) {
            Map<Byte, ReverseMappingEntity> ptr = rev;
            Character ch = e.getKey();
            for (int i = 0; i < e.getValue().length; i++) {
                Byte b = e.getValue()[i];
                ReverseMappingEntity ent = ptr.get(b);
                if (ent == null) {
                    ent = new ReverseMappingEntity();
                    ptr.put(b, ent);
                }
                if (i + 1 == e.getValue().length) {
                    ent.setCharacter(ch);
                } else {
                    ptr = ent.getMapping();
                }
            }
        }
        return rev;
    }

    @Override
    public boolean canEncode() {
        return true;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    @Override
    public boolean contains(Charset cs) {
        return displayName().equals(cs.displayName());
    }

    private static class ReverseMappingEntity {
        private TreeMap<Byte, ReverseMappingEntity> mapping = new TreeMap<>();
        private Character character;

        public Character getCharacter() {
            return character;
        }

        public void setCharacter(Character ch) {
            this.character = ch;
        }

        public Map<Byte, ReverseMappingEntity> getMapping() {
            return mapping;
        }
    }

    private class Decoder extends CharsetDecoder {
        private LinkedList<Byte> buffer = new LinkedList<>();

        Decoder(Charset charset) {
            super(charset, 2.2f, 3.0f);
        }

        @Override
        protected CoderResult decodeLoop(final ByteBuffer in, CharBuffer out) {
            ReverseMappingBuffer rmb = new ReverseMappingBuffer(reverseMapping, buffer) {
                @Override
                protected Byte onNextByte() {
                    return in.hasRemaining() ? in.get() : null;
                }
            };
            while (in.hasRemaining() || rmb.hasRemaining()) {
                if (out.hasRemaining()) {
                    Character ch = rmb.nextCharacter();
                    out.append(ch);
                } else {
                    return CoderResult.OVERFLOW;
                }
            }
            return CoderResult.UNDERFLOW;
        }
    }

    private class Encoder extends CharsetEncoder {

        Encoder(Charset charset) {
            super(charset, 2.2f, 3.0f);
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            while (in.hasRemaining()) {
                if (out.hasRemaining()) {
                    char unicode = in.get();
                    byte[] ansel;
                    if (unicode <= 0x7f) {
                        ansel = new byte[2];
                        ansel[0] = (byte) ((unicode >> 8) & 0xff);
                        ansel[1] = (byte) ((unicode) & 0xff);
                    } else {
                        ansel = mapping.get(unicode);
                        if (ansel == null) {
                            return CoderResult.unmappableForLength(2);
                        }
                    }
                    boolean started = false;
                    for (int i = 0; i < ansel.length; i++) {
                        if (started || ansel[i] != 0 || i == ansel.length - 1) {
                            out.put(ansel[i]);
                            started = true;
                        }
                    }
                } else {
                    return CoderResult.OVERFLOW;
                }
            }
            return CoderResult.UNDERFLOW;
        }
    }

    abstract class ReverseMappingBuffer {
        private Map<Byte, ReverseMappingEntity> rm;
        private LinkedList<Byte> buffer;

        ReverseMappingBuffer(Map<Byte, ReverseMappingEntity> rm, LinkedList<Byte> buffer) {
            this.rm = rm;
            this.buffer = buffer;
        }

        boolean hasRemaining() {
            return !buffer.isEmpty();
        }

        Character nextCharacter() {
            LinkedList<Byte> queue = new LinkedList<>();
            ReverseMappingEntity rme = null;
            Character ch = null;
            for (Byte b = nextByte(); b != null; b = nextByte()) {
                queue.addLast(b);
                rme = rme != null ? rme.getMapping().get(b) : rm.get(b);
                if (rme == null) {
                    buffer.addAll(queue);
                    return ch != null ? ch : Character.valueOf((char) nextByte().byteValue());
                }
                if (rme.getCharacter() != null) {
                    ch = rme.getCharacter();
                    queue.clear();
                }
            }
            return ch;
        }

        protected abstract Byte onNextByte();

        private Byte nextByte() {
            if (!buffer.isEmpty()) {
                return buffer.pollFirst();
            } else {
                return onNextByte();
            }
        }
    }
}
