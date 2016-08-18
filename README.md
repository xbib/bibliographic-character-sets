# Bibliographic character sets

This is a collection of bibliographic character sets implemented in 
Java.

These character sets have not been included in the standard Java 
distribution. Most of the character sets predate Unicode and are 
dormant now but are still in active use in library application 
system software.

The reason to provide these character sets is to assist the public 
in migrating library data to Unicode, and UTF-8, respectively.

## Usage

With Maven

    <dependency>
        <groupId>org.xbib</groupId>
        <artifactId>bibliographic-character-sets</artifactId>
        <version>1.0.0</version>
    </dependency>

With Gradle

    configurations {
      provided
    }
    dependencies {
       provided 'org.xbib:bibliographic-character-sets:1.0.0'
    }

You can also include this jar in the classpath, the Java CharsetProvider and
ServiceLoader API will then make the character sets available, 
e.g. by `Charset.forName(name)`

This is free software. 
Please follow the AGPL license, which requires to offer the source code
of your project to the public if you make modifications to this program.

All contributions and pull requests are welcome.

If you have questions or find issues, pleas post them at
https://github.com/xbib/bibliographic-character-sets/issues

## List of character sets included

### ANSEL "ANSI/NISO Z39.47-1993 (R2003) Extended Latin Alphabet Coded Character Set for Bibliographic Use (ANSEL)"

This implementation can only decode from ANSEL / Z39.47.

Included are the following sets specified by the Library of Congress at
https://www.loc.gov/marc/specifications/specchartables.html 

Basic Latin (ASCII), Extended Latin (ANSEL),  Greek Symbols,
Subscripts, Superscripts, Basic Hebrew, Basic Cyrillic,
Extended Cyrillic, Basic Arabic, Extended Arabic,
Basic Greek, Chinese, Japanese, Korean (EACC)
 
Usage:
 
     Charset.forName("ANSEL")
 
### ISO 5426 "Extension of the Latin alphabet coded character set for bibliographic information interchange"

Usage:
 
     Charset.forName("ISO-5426")

### ISO 5428 "Greek alphabet coded character set for bibliographic information interchange"

Usage:
 
     Charset.forName("ISO-5428")

### Pica (a variant of the INTERMARC character set, a 1979 french/danish adoption of USMARC/UKMARC)

Usage:
 
     Charset.forName("PICA")

### MAB-Diskette (a variant of CP850 character set)

Usage:
 
     Charset.forName("MAB-DISKETTE")

### US-ASCII (re-implementation for demonstration purpose, disabled by default)

See also the CREDITS.txt for acknowledgements.

# License

Copyright (C) 2016 Jörg Prante and the xbib organization

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.