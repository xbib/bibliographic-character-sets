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
 *
 * Derived from
 *
 * ByteCharset.java -- Abstract class for generic 1-byte encodings.
 * Copyright (C) 2005 Free Software Foundation, Inc.
 *
 * This file is part of GNU Classpath.
 *
 * GNU Classpath is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * GNU Classpath is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GNU Classpath; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 *
 * Derived from
 *
 * ByteCharset.java -- Abstract class for generic 1-byte encodings.
 * Copyright (C) 2005 Free Software Foundation, Inc.
 *
 *  This file is part of GNU Classpath.
 *
 *  GNU Classpath is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 *  GNU Classpath is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 * along with GNU Classpath; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 *
 *  Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 *  As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.xbib.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * A generic encoding framework for single-byte encodings, utilizing a look-up
 * table. This replaces the gnu.java.io.EncoderEightBitLookup class, created by
 * Aron Renn.
 */
abstract class ByteCharset extends Charset {

    /**
     * Char to signify the character in the table is undefined.
     */
    private static final char NONE = (char) 0xFFFD;
    char[] lookupTable;

    ByteCharset(String canonicalName, String[] aliases) {
        super(canonicalName, aliases);
    }

    /**
     * Most western charsets include ASCII, but this should be overloaded for
     * others.
     */
    public boolean contains(Charset cs) {
        return cs instanceof ASCII || cs.getClass() == getClass();
    }

    private char[] getLookupTable() {
        return lookupTable;
    }

    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    private static final class Decoder extends CharsetDecoder {

        private final char[] lookup;

        Decoder(ByteCharset cs) {
            super(cs, 1.0f, 1.0f);
            lookup = cs.getLookupTable();
        }

        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            while (in.hasRemaining()) {
                byte b = in.get();
                char c;
                if (!out.hasRemaining()) {
                    in.position(in.position() - 1);
                    return CoderResult.OVERFLOW;
                }
                c = lookup[b & 0xFF];
                out.put(c);
            }

            return CoderResult.UNDERFLOW;
        }
    }

    private static final class Encoder extends CharsetEncoder {

        private final byte[] lookup;

        Encoder(ByteCharset cs) {
            super(cs, 1.0f, 1.0f);
            char[] lookuptable = cs.getLookupTable();
            int max = 0;
            for (char ch : lookuptable) {
                max = (int) ch > max && (int) ch < NONE ? (int) ch : max;
            }
            lookup = new byte[max + 1];
            for (int i = 0; i < lookuptable.length; i++) {
                int c = lookuptable[i];
                if (c != 0 && c < NONE) {
                    lookup[c] = (byte) i;
                }
            }
        }

        protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            while (in.hasRemaining()) {
                int c = in.get();
                if (!out.hasRemaining()) {
                    in.position(in.position() - 1);
                    return CoderResult.OVERFLOW;
                }
                byte b = c < lookup.length ? lookup[c] : (byte) 0;
                if ((int) b != 0 || c == 0) {
                    out.put(b);
                } else {
                    in.position(in.position() - 1);
                    return CoderResult.unmappableForLength(1);
                }
            }
            return CoderResult.UNDERFLOW;
        }
    }
}
