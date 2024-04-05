/**
 * 
 * Copyright 2024 California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.opencaesar.owl.save;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;

/**
 * The <b>Catalog</b> that resolves logical IRIs to physical URIs. It is a
 * wrapper around the the Apache XML Resolver Catalog.
 * 
 * @author elaasar
 */
public final class OwlCatalog {

    /*
     * The singleton catalog manager
     */
    private static CatalogManager manager = new CatalogManager();
    static {
        manager.setUseStaticCatalog(false);
        manager.setIgnoreMissingProperties(true);
    }

    /*
     * The wrapped Apache catalog
     */
    private CatalogEx catalog;

    /*
     * Creates a new OmlCatalog instance
     */
    private OwlCatalog(CatalogEx catalog) {
        this.catalog = catalog;
    }

    /**
     * Creates a new Oml Catalog given a catalog URI and a list of file extensions
     * 
     * @param catalogFile    The catalog file
     * @return A new instance of Oml Catalog
     * @throws IOException When there are problems parsing the catalog
     */
    public static OwlCatalog create(File catalogFile) throws IOException {
        CatalogEx catalog = new CatalogEx(catalogFile.toURI());
        catalog.setCatalogManager(manager);
        catalog.setupReaders();
        catalog.loadSystemCatalogs();
        catalog.parseCatalog(catalogFile.toString());
        return new OwlCatalog(catalog);
    }

    /**
     * Resolves the given URI to a file path
     * 
     * @param uri The URI to resolve
     * @return The resolved file path
     */
    public String resolveURI(String uri) {
        try {
            String resolved = catalog.resolveURI(uri);
            if (resolved != null) {
            	resolved = normalize(resolved);
            } else {
            	System.out.println(uri+" cannot be resolved");
            }
            return resolved;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String normalize(String path) {
        java.net.URI uri = java.net.URI.create(path);
        java.net.URI normalized = uri.normalize();
        return path.replaceFirst(uri.getRawPath(), normalized.getRawPath());
    }

    private static class CatalogEx extends Catalog {
        private URI baseUri;

        public CatalogEx(URI catalogUri) {
            String s = catalogUri.toString();
            int i = s.lastIndexOf("/");
            try {
                this.baseUri = new URI(s.substring(0, i));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String makeAbsolute(String sysid) {
            sysid = fixSlashes(sysid);
            return baseUri.toString() + '/' + sysid;
        }
    }

}
