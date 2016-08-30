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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

class AnselCodeTableParser {

    private static final Logger logger = Logger.getLogger(AnselCodeTableParser.class.getName());

    private final List<CodeTable> codeTables;

    private CodeTable codeTable;

    private CharacterSet characterSet;

    private Code code;

    private StringBuilder content;

    AnselCodeTableParser(InputStream inputStream) {
        List<CodeTable> codeTables;
        try {
            codeTables = createCodeTables(inputStream);
        } catch (XMLStreamException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            codeTables = null;
        }
        this.codeTables = codeTables;
    }

    public List<CodeTable> getCodeTables() {
        return codeTables;
    }

    private List<CodeTable> createCodeTables(InputStream inputStream) throws XMLStreamException {
        List<CodeTable> codetables = new LinkedList<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader xmlReader = factory.createXMLEventReader(inputStream);
        while (xmlReader.hasNext()) {
            processEvent(codetables, xmlReader.peek());
            xmlReader.nextEvent();
        }
        return codetables;
    }

    private void processEvent(List<CodeTable> codetables, XMLEvent event) {
        if (event.isStartDocument()) {
            this.code = new Code();
            this.content = new StringBuilder();
        }
        if (event.isStartElement()) {
            StartElement element = (StartElement) event;
            String name = element.getName().getLocalPart();
            switch (name) {
                case "codeTables": {
                    // ignore
                    break;
                }
                case "codeTable": {
                    this.codeTable = new CodeTable();
                    break;
                }
                case "characterSet": {
                    this.characterSet = new CharacterSet();
                    @SuppressWarnings("unchecked")
                    Iterator<Attribute> it = element.getAttributes();
                    while (it.hasNext()) {
                        Attribute attr = it.next();
                        QName attributeName = attr.getName();
                        String attributeLocalName = attributeName.getLocalPart();
                        String attributeValue = attr.getValue();
                        if ("name".equals(attributeLocalName)) {
                            characterSet.name = attributeValue;
                        } else if ("isoCode".equals(attributeLocalName)) {
                            characterSet.isoCode = attributeValue;
                        }
                    }
                    break;
                }
                case "code": {
                    code = new Code();
                    break;
                }
                default:
                    break;
            }
        } else if (event.isCharacters()) {
            Characters c = (Characters) event;
            if (!c.isIgnorableWhiteSpace()) {
                // character events may come more than once (e.g. because of XML entities like &quot;)
                // concatenate with values that might exist
                content.append(c.getData());
            }
        } else if (event.isEndElement()) {
            EndElement element = (EndElement) event;
            String name = element.getName().getLocalPart();
            switch (name) {
                case "codeTable": {
                    codetables.add(codeTable);
                    codeTable = new CodeTable();
                    break;
                }
                case "characterSet": {
                    codeTable.add(characterSet);
                    characterSet = new CharacterSet();
                    break;
                }
                case "code": {
                    characterSet.add(code);
                    code = new Code();
                    break;
                }
                case "marc": {
                    String s = content.toString().trim();
                    char[] ch = new char[s.length() / 2];
                    for (int i = 0; i < s.length(); i += 2) {
                        ch[i / 2] = (char) ((Character.digit(s.charAt(i), 16) << 4)
                                + Character.digit(s.charAt(i + 1), 16));
                    }
                    code.marc = new String(ch);
                    break;
                }
                case "ucs": {
                    String s = content.toString().trim();
                    // two chars have no ucs equivalent...
                    if (!s.isEmpty()) {
                        code.ucs = (char) (Integer.parseInt(s, 16) & 0xFFFF);
                    }
                    break;
                }
                case "utf-8": {
                    String s = content.toString().trim();
                    char[] ch = new char[s.length() / 2];
                    for (int i = 0; i < s.length(); i += 2) {
                        ch[i / 2] = (char) ((Character.digit(s.charAt(i), 16) << 4)
                                + Character.digit(s.charAt(i + 1), 16));
                    }
                    code.utf8 = new String(ch);
                    break;
                }
                case "name": {
                    code.name = content.toString();
                    break;
                }
                case "isCombining": {
                    code.isCombining = "true".equals(content.toString());
                    break;
                }
                default:
                    break;
            }
            content.setLength(0);
        }
    }

    static class CodeTable {
        private final List<CharacterSet> characterSets = new LinkedList<>();

        void add(CharacterSet characterSet) {
            characterSets.add(characterSet);
        }

        List<CharacterSet> getCharacterSets() {
            return characterSets;
        }
    }

    static class CharacterSet {
        String name;
        String isoCode;
        int length;
        Map<String, Code> marc = new HashMap<>();
        Map<Character, Code> unicode = new HashMap<>();

        void add(Code code) {
            marc.putIfAbsent(code.marc, code);
            length = code.marc.length();
            unicode.putIfAbsent(code.ucs, code);
        }

        String getName() {
            return name;
        }

        int getLength() {
            return length;
        }

        String getIsoCode() {
            return isoCode;
        }

        Map<String, Code> getMarc() {
            return marc;
        }

        Map<Character, Code> getUnicode() {
            return unicode;
        }
    }

    static class Code {
        // Universal Character Set (UCS, ISO-IEC 10646)/Unicode, always 16 bit
        char ucs;
        // MARC-8 standard (single char) or EACC 24-bit code (three chars)
        String marc;
        // UTF-8 code (in hex), 1-3 bytes
        String utf8;
        // name
        String name;
        boolean isCombining;

        char getUcs() {
            return ucs;
        }

        String getMarc() {
            return marc;
        }

        String getUtf8() {
            return utf8;
        }

        String getName() {
            return name;
        }

        boolean isCombining() {
            return isCombining;
        }

        public String toString() {
            return "marc=" + marc + " isCombining=" + isCombining + " ucs=" + ucs;
        }
    }
}

