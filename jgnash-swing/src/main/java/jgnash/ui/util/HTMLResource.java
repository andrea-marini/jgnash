/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2015 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.ui.util;

import java.net.URL;
import java.util.Locale;

/**
 * This class is for returning URL's to localized html files. At this time it does not support variants or countries...
 * only language.
 * 
 * @author Craig Cavanaugh
 *
 */
public class HTMLResource {

    private static final String ROOTPATH = "/jgnash/resource/html/";

    private static final String DEFAULT = "/jgnash/resource/html/en/";

    /**
     * Contains only static methods
     */
    private HTMLResource() {
    }

    /**
     * Find a locale specific html file given the file name.
     * 
     * @param fileName the file name of the html file to look for.
     * @return a URL for reading the resource, or null if the resource could not be found
     */
    public static URL getURL(final String fileName) {
        String lang = Locale.getDefault().getLanguage();

        URL url = Object.class.getResource(ROOTPATH + lang + "/" + fileName);

        if (url != null) {
            return url;
        }

        // try to get the default
        return Object.class.getResource(DEFAULT + fileName);
    }

    /**
     * Find a locale specific html file given the file name.
     * 
     * @param fileName the file name of the html file to look for.
     * @return a URL for reading the resource, or null if the resource could not be found
     *//*
    public static InputStream getInputStream(final String fileName) {
        String lang = Locale.getDefault().getLanguage();

        InputStream inputStream = Object.class.getResourceAsStream(ROOTPATH + lang + "/" + fileName);

        if (inputStream != null) {
            return inputStream;
        }

        // try to get the default
        return Object.class.getResourceAsStream(DEFAULT + fileName);
    }*/
}
