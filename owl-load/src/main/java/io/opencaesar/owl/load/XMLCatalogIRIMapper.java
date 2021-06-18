package io.opencaesar.owl.load;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

    private static CatalogManager manager = new CatalogManager();

    private Catalog catalog;

	private final List<String> extensions;

    public static Catalog create(URL catalogUrl) throws IOException {
        Catalog catalog = new Catalog();
        catalog.setCatalogManager(manager);
        catalog.setupReaders();
        catalog.loadSystemCatalogs();
        catalog.parseCatalog(catalogUrl);
        return catalog;
    }

	public XMLCatalogIRIMapper(File catalogFile, List<String> extensions) throws IOException {
		this.extensions = extensions;
		if (null == catalogFile || !catalogFile.isFile() || !catalogFile.isAbsolute())
			throw new IllegalArgumentException("The catalogFile must exists and be an absolute path: " + catalogFile);
		this.catalog = create(catalogFile.toURI().toURL());
	}

	@Override
	public IRI getDocumentIRI(IRI originalIri) {
		try {
			String documentUri = catalog.resolveURI(originalIri.toString());
			if (documentUri != null && documentUri.startsWith("file:")) {
				String documentPath = documentUri.substring(5);
				File documentFile = new File(documentPath);
				if (documentFile.exists() && documentFile.isFile())
					return IRI.create(documentUri);
				
				String extension = FilenameUtils.getExtension(documentPath);
				if (extension.isEmpty() || StringUtils.isNumeric(extension)) {
					for ( String ext : extensions ) {
						String uri = (ext.startsWith(".")) ? documentPath+ext : documentPath+"." + ext;
						File f = new File(uri);
						if (f.exists() && f.isFile())
							return IRI.create("file:" + uri);
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

}
