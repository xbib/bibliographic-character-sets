module org.xbib.charset {
    exports org.xbib.charset;
    requires java.xml;
    provides java.nio.charset.spi.CharsetProvider with
            org.xbib.charset.BibliographicCharsetProvider;
}
