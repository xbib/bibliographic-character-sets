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

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.spi.CharsetProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extra bibliographic character sets.
 */
public class BibliographicCharsetProvider extends CharsetProvider {

    private static final Logger logger = Logger.getLogger(BibliographicCharsetProvider.class.getName());

    /**
     * The reference to the character set instance.
     * If there are no remaining references to this instance,
     * the character set will be removed by the garbage collector.
     */
    private static volatile SoftReference<BibliographicCharsetProvider> instance = null;
    private final Map<String, String> classMap;
    private final Map<String, String> aliasMap;
    private final Map<String, String[]> aliasNameMap;
    private final Map<String, SoftReference<Charset>> cache;
    private final String packagePrefix;

    /**
     * Constructor must be public.
     */
    public BibliographicCharsetProvider() {
        classMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        aliasMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        aliasNameMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        cache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        packagePrefix = getClass().getPackage().getName();

        charset("ANSEL", "AnselCharset",
                new String[]{"ANSI_Z39_47", "ANSI-Z39-47", "Z39_47", "Z39-47", "ansel", "usmarc", "usm94"});
        charset("ISO-5426", "ISO5426", new String[]{"x-mab", "x-MAB", "ISO-5426", "ISO_5426", "ISO_5426:1983", "MAB2"});
        charset("ISO-5428", "ISO5428", new String[]{"ISO_5428", "ISO-5428:1984", "iso-ir-55"});
        charset("MAB-Diskette", "MabDisketteCharset", new String[]{});
        charset("PICA", "Pica", new String[]{"Pica", "pica"});
        charset("x-PICA", "PicaCharset", new String[]{"x-pica"});
        charset("SIMPLE_ANSEL", "SimpleAnselCharset", new String[]{});
        instance = new SoftReference<>(this);
    }

    /**
     * List all aliases defined for a character set.
     *
     * @param s the name of the character set
     * @return an alias string array
     */
    static String[] aliasesFor(String s) {
        SoftReference<BibliographicCharsetProvider> softreference = instance;
        BibliographicCharsetProvider charsets = null;
        if (softreference != null) {
            charsets = softreference.get();
        }
        if (charsets == null) {
            charsets = new BibliographicCharsetProvider();
            instance = new SoftReference<>(charsets);
        }
        return charsets.aliases(s);
    }

    @Override
    public final Charset charsetForName(String s) {
        return lookup(canonicalize(s));
    }

    @Override
    public final Iterator<Charset> charsets() {
        return new Iterator<Charset>() {

            Iterator<String> iterator = classMap.keySet().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Charset next() {
                return lookup(iterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void charset(String name, String className, String[] aliases) {
        classMap.putIfAbsent(name, className);
        for (String alias : aliases) {
            aliasMap.putIfAbsent(alias, name);
        }
        aliasNameMap.putIfAbsent(name, aliases);
    }

    private String canonicalize(String charsetName) {
        String aliasCharsetName = aliasMap.get(charsetName);
        return aliasCharsetName != null ? aliasCharsetName : charsetName;
    }

    private Charset lookup(String charsetName) {
        SoftReference<Charset> softreference = cache.get(charsetName);
        if (softreference != null) {
            Charset charset = softreference.get();
            if (charset != null) {
                return charset;
            }
        }
        String className = classMap.get(charsetName);
        if (className == null) {
            return null;
        }
        try {
            Class<?> cl = Class.forName(packagePrefix + "." + className, true, getClass().getClassLoader());
            Charset charset = (Charset) cl.newInstance();
            cache.put(charsetName, new SoftReference<>(charset));
            return charset;
        } catch (ClassNotFoundException e1) {
            logger.log(Level.WARNING, "Class not found: " + packagePrefix + "." + className);
        } catch (IllegalAccessException e2) {
            logger.log(Level.WARNING, "Illegal access: " + packagePrefix + "." + className);
        } catch (InstantiationException e3) {
            logger.log(Level.WARNING, "Instantiation failed: " + packagePrefix + "." + className);
        }
        return null;
    }

    private String[] aliases(String s) {
        return (String[]) aliasNameMap.get(s);
    }
}
