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

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 *
 */
public class AnselCharsetTest {

    @Test
    public void testAnsel() throws Exception {
        ByteBuffer buf = ByteBuffer.wrap("\u00e8\u0075".getBytes(StandardCharsets.ISO_8859_1));
        Charset charset = Charset.forName("ANSEL");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer cbuf = decoder.decode(buf);
        String s = cbuf.toString();
        assertEquals("\u0075\u0308", s);
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        assertEquals("ü", s);
    }

    @Test
    public void testAnsel2() throws Exception {
        ByteBuffer buf =
                ByteBuffer.wrap("\u00AC\u00E2\u0041\u00ED\u0042\u00E2\u0043\u00E2\u0044".getBytes(StandardCharsets.ISO_8859_1));
        Charset charset = Charset.forName("ANSEL");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer cbuf = decoder.decode(buf);
        String s = cbuf.toString();
        assertEquals(9, s.length());
        s = Normalizer.normalize(s, Normalizer.Form.NFC);
        assertEquals("ƠÁB̕ĆD́", s);
        assertEquals(7, s.length());
    }

}
