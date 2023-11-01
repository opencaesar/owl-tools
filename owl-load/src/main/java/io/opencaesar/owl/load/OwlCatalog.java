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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
     * The file extensions
     */
    private List<String> fileExtensions;
    
    /*
     * Creates a new OmlCatalog instance
     */
    private OwlCatalog(CatalogEx catalog, List<String> fileExtensions) {
        this.catalog = catalog;
        this.fileExtensions = fileExtensions;
    }

    /**
     * Creates a new Oml Catalog given a catalog URI and a list of file extensions
     * 
     * @param catalogFile The catalog file
     * @param fileExtensions The file extensions
     * @return A new instance of Oml Catalog
     * @throws IOException When there are problems parsing the catalog
     */
    public static OwlCatalog create(File catalogFile, List<String> fileExtensions) throws IOException {
    	CatalogEx catalog = new CatalogEx(catalogFile.toURI());
        catalog.setCatalogManager(manager);
        catalog.setupReaders();
        catalog.loadSystemCatalogs();
        catalog.parseCatalog(catalogFile.toString());
        return new OwlCatalog(catalog, fileExtensions);
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
	    	resolved = normalize(resolved);
			if (resolved != null && resolved.startsWith("file:")) {
				File f = new File(new URI(resolved));
				if (!f.exists() || !f.isFile()) {
					for ( String ext : fileExtensions ) {
						String fileWithExtensionPath = f.toString()+"." + ext;
						File f_ext = new File(fileWithExtensionPath);
						if (f_ext.exists() && f_ext.isFile())
							return resolved+"."+ext;
					}
				}
			}
			return resolved;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    /**
     * Deresolves the given file path to a URI
     * 
     * @param path The file path to resolve
     * @return The resolved URI
     */
    public String deresolveURI(String path) {
		try {
			int i = path.lastIndexOf('.');
			if (i != -1) {
				path = path.substring(0, i);   
			}
		    String startString = null;
		    String prefix = null;
			for (Map.Entry<String, URI> e : getRewriteRules().entrySet()) {
				String p = new File(e.getValue()).toString() + "/";
				if (p.length() <= path.length() && p.equals(path.substring(0, p.length()))) {
					// Is this the longest prefix?
					if (startString == null || p.length() > startString.length()) {
						startString = p;
						prefix = e.getKey();
					}
				}
			}
			return (prefix!=null) ? prefix + path.substring(startString.length()) : null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * Gets the current base of the catalog
     * 
     * @return The current base of the catalog
     */
    public String getCurrentBase() {
        return catalog.getCurrentBase();
    }

    /**
     * Gets the base URI
     * 
     * @return The base URI
     */
    public URI getBaseUri() {
    	return catalog.getBaseUri();
    }
    
    /**
     * Gets the catalog entries
     * 
     * @return The entries of the catalog
     */
    public List<CatalogEntry> getEntries() {
        List<CatalogEntry> entries = new ArrayList<CatalogEntry>();
        Enumeration<?> en = catalog.getCatalogEntries().elements();
        while (en.hasMoreElements()) {
            entries.add((CatalogEntry) en.nextElement());
        }
        return entries;
    }

    /**
     * Gets the nested catalogs of this catalog
     * 
     * @return The nested catalog of this catalog
     */
    public List<String> getNestedCatalogs() {
        List<String> entries = new ArrayList<String>();
        Enumeration<?> en = catalog.getCatalogs().elements();
        while (en.hasMoreElements()) {
            entries.add((String)en.nextElement());
        }
        return entries;
    }

    /**
     * Gets the URIs that are used for rewrite rules in this catalog
     * 
     * @return a map of rewrite URIs
     */
    public Map<String, URI> getRewriteRules() {
		var rewriteUris = new HashMap<String, URI>();
		for (CatalogEntry e : getEntries()) {
			if (e.getEntryType() == Catalog.REWRITE_URI) { // only type of entry supported so far
				var uri = URI.create(normalize(e.getEntryArg(1)));
	    		String s = uri.toString();
				if (s.endsWith("/")) {
		    		try {
						uri = new URI(s.substring(0, s.length()-1));
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}
				rewriteUris.put(e.getEntryArg(0), uri);
			}
		}
    	return rewriteUris;
    }
    
    private String normalize(String path) {
    	java.net.URI uri = java.net.URI.create(path);
    	java.net.URI normalized = uri.normalize();
    	return path.replaceFirst(uri.getRawPath(), normalized.getRawPath());
    }
    
    /**
     * Gets the URIs of files that are mapped by this catalog
     * 
     * @return a list of file URIs
     */
    public Map<String, URI> getFileUriMap() {
		var uris = new HashMap<String, URI>();
		for (final var rewriteRule : getRewriteRules().entrySet()) {
			var rewriteUri = rewriteRule.getValue();
			var path = new File(rewriteUri);
			if (path.isDirectory()) {
				for (var file : getFiles(path)) {
					String relative = path.toURI().relativize(file.toURI()).getPath();
					uris.put(rewriteRule.getKey()+trimFileExtension(relative), URI.create(rewriteUri+"/"+relative));
				}
			} else { // likely a file name with no extension
				for (String ext : fileExtensions) {
					var file = new File(path.toString()+"."+ext);
					if (file.exists()) {
						uris.put(rewriteRule.getKey(), URI.create(path.toString()+"."+ext));
						break;
					}
				}
			}
		}
		return uris;
    }
    
    private List<File> getFiles(File folder) {
		final var files = new LinkedHashSet<File>();
		for (File file : folder.listFiles()) {
			if (file.isFile()) {
				var ext = getFileExtension(file);
				if (fileExtensions.contains(ext)) {
					files.add(file);
				}
			} else if (file.isDirectory()) {
				files.addAll(getFiles(file));
			}
		}
		return new ArrayList<File>(files);
	}

	private String getFileExtension(final File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1)
        	return fileName.substring(fileName.lastIndexOf(".")+1);
        else 
        	return "";
	}

	private String trimFileExtension(final String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i != -1)
        	return fileName.substring(0, i);
        else 
        	return fileName;
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
    	URI getBaseUri() {
    		return baseUri;
    	}
        Vector<?> getCatalogEntries() {
            return catalogEntries;
        }
        Vector<?> getCatalogs() {
            return catalogs;
        }
        @Override
        protected String makeAbsolute(String sysid) {
            sysid = fixSlashes(sysid);
            return  baseUri.toString()+'/'+sysid;
        }
    }

}
