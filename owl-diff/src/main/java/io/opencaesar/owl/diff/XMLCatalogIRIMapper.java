package io.opencaesar.owl.diff;

import java.io.File;
import java.io.IOException;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

	private final Catalog catalog;

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
				String filePath = documentUri.substring(5); // remove 'file:'
				File f = new File(filePath);
				if (!f.exists() || !f.isFile()) {
					String fileWithExtensionPath = filePath+".owl";
					f = new File(fileWithExtensionPath);
					if (f.exists() && f.isFile())
						return IRI.create("file:" + fileWithExtensionPath);
				}
			}
			return IRI.create(documentUri);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

}
