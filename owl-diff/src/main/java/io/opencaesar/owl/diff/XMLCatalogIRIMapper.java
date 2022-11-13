package io.opencaesar.owl.diff;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

/**
 * Utility for using an Oasis XML Catalog for mapping Ontology IRIs.
 */
@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

	/**
	 * an Oasis XML Catalog
	 */
	private final Catalog catalog;

	/**
	 * Constructor
	 * @param catalogFile an Oasis XML Catalog file
	 * @throws IOException error
	 */
	public XMLCatalogIRIMapper(File catalogFile) throws IOException {
		if (null == catalogFile || !catalogFile.isFile() || !catalogFile.isAbsolute())
			throw new IllegalArgumentException("The catalogFile must exists and be an absolute path: " + catalogFile);
		CatalogManager manager = new CatalogManager();
		manager.setUseStaticCatalog(false);
		manager.setIgnoreMissingProperties(true);
		catalog = manager.getCatalog();
		catalog.setupReaders();
		catalog.parseCatalog(catalogFile.toURI().toURL());
	}

	@Override
	public IRI getDocumentIRI(IRI originalIri) {
		try {
			String documentUri = catalog.resolveURI(originalIri.toString());
			if (documentUri != null && documentUri.startsWith("file:")) {
				File f = new File(new URI(documentUri));
				if (!f.exists() || !f.isFile()) {
					String fileWithExtensionPath = f.toString()+".owl";
					File f_ext = new File(fileWithExtensionPath);
					if (f_ext.exists() && f_ext.isFile())
						return IRI.create(documentUri+".owl");
				}
			}
			return IRI.create(documentUri);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

}
