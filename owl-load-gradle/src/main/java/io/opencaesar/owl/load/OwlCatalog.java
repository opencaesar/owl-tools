/**
 * 
 * Copyright 2019-2021 California Institute of Technology ("Caltech").
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
package io.opencaesar.owl.load;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogEntry;
import org.apache.xml.resolver.CatalogManager;

/**
 * The <b>Catalog</b> that resolves logical IRIs to physical URIs. It is a wrapper around the the Apache XML Resolver Catalog. 
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
     * Creates a new OwlCatalog instance
     */
    private OwlCatalog(CatalogEx catalog) {
        this.catalog = catalog;
    }

    /**
     * Creates a new Owl Catalog given a catalog path
     * 
     * @param catalogPath The path of a catalog file named 'catalog.xml'
     * @return A new instance of Owl Catalog
     * @throws IOException When there are problems parsing the catalog
     */
    public static OwlCatalog create(URI catalogPath) throws IOException {
    	CatalogEx catalog = new CatalogEx();
        catalog.setCatalogManager(manager);
        catalog.setupReaders();
        catalog.loadSystemCatalogs();
        catalog.parseCatalog(catalogPath.toURL());
        return new OwlCatalog(catalog);
    }

    /**
     * Gets the catalog entries
     * 
     * @return The entries of the catalog
     */
    private List<CatalogEntry> getEntries() {
        List<CatalogEntry> entries = new ArrayList<CatalogEntry>();
        Enumeration<?> en = catalog.getCatalogEntries().elements();
        while (en.hasMoreElements()) {
            entries.add((CatalogEntry) en.nextElement());
        }
        return entries;
    }

    /**
     * Gets the URIs that are used for rewrite rules in this catalog
     * 
     * @return a list of rewrite URIs
     */
    private List<URI> getRewriteUris() throws URISyntaxException {
		var rewriteUris = new ArrayList<URI>();
		for (CatalogEntry e : getEntries()) {
			if (e.getEntryType() == Catalog.REWRITE_URI) { // only type of entry supported so far
				var uri = e.getEntryArg(1);
				if (uri.endsWith("/")) {
					uri = uri.substring(0, uri.length()-1);
				}
				rewriteUris.add(new URI(uri));
			}
		}
    	return rewriteUris;
    }

    private Set<File> getFiles(File folder, List<String> fileExtensions) {
		final var files = new HashSet<File>();
		for (File file : folder.listFiles()) {
			if (file.isFile()) {
				var ext = FilenameUtils.getExtension(file.toString());
				if (fileExtensions.contains(ext)) {
					files.add(file);
				}
			} else if (file.isDirectory()) {
				files.addAll(getFiles(file, fileExtensions));
			}
		}
		return files;
	}

    /**
     * Gets the URIs of files that are mapped by this catalog
     * 
     * @param fileExtensions the list of file extensions
     * @return a list of file URIs
     * @throws URISyntaxException when the catalog rewrite URIs are ill-formed
     */
    public List<URI> getFileUris(List<String> fileExtensions) throws URISyntaxException {
		var uris = new ArrayList<URI>();
		for (final URI rewriteUri : getRewriteUris()) {
			var path = new File(rewriteUri.getPath());
			if (path.isDirectory()) {
				for (var file : getFiles(path, fileExtensions)) {
					String relative = path.toURI().relativize(file.toURI()).getPath();
					uris.add(new URI(rewriteUri+"/"+relative));
				}
			} else { // likely a file name with no extension
				for (String ext : fileExtensions) {
					var file = new File(path.toString()+"."+ext);
					if (file.exists()) {
						uris.add(new URI(rewriteUri+"."+ext));
						break;
					}
				}
			}
		}
		return uris;
    }
    
    private static class CatalogEx extends Catalog {
        Vector<?> getCatalogEntries() {
            return catalogEntries;
        }
    }
}
