package io.opencaesar.owl.diff;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

	private Catalog catalog;
	private URI baseIRI;

	public XMLCatalogIRIMapper(String catalogPath) throws MalformedURLException, IOException {
		final File catalogFile = new File(catalogPath);
		CatalogManager manager = new CatalogManager();
		manager.setUseStaticCatalog(false);
		manager.setIgnoreMissingProperties(true);
		catalog = manager.getCatalog();
		catalog.setupReaders();
		catalog.parseCatalog(catalogFile.toURI().toURL());
		baseIRI = new File(catalog.getCurrentBase()).getParentFile().toURI();
	}

	@Override
	public IRI getDocumentIRI(IRI original) {
		try {
			String iri = catalog.resolveURI(original.toString());
			if (iri != null) {
				URI redirect = new URI(iri);
				if (redirect.toString().startsWith("file:.")) { 
					//some catalogs erroneously treat paths that start with 'file:.' as relative
					redirect = baseIRI.resolve(new URI(redirect.getSchemeSpecificPart()));
				}
				// add ".owl" to the end of import paths
				if (!redirect.toString().endsWith(".owl")) {
					redirect = new URI(redirect.toString()+".owl");
				}
				return IRI.create(redirect);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

}
