package io.opencaesar.owl.reason;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

	private Catalog catalog;

	public XMLCatalogIRIMapper(String catalogPath) throws MalformedURLException, IOException {
		final File catalogFile = new File(catalogPath);
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
			if (documentUri != null) {
				String extension = FilenameUtils.getExtension(documentUri);
				// add ".owl" to the end of import paths
				if (extension == null || extension.isEmpty() || StringUtils.isNumeric(extension)) {
					documentUri = documentUri+".owl";
				}
				return IRI.create(documentUri);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

}
