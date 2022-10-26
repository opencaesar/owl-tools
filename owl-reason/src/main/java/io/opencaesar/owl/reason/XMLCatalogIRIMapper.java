package io.opencaesar.owl.reason;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

	private final Catalog catalog;

	private final List<String> extensions;

	public XMLCatalogIRIMapper(File catalogFile, List<String> extensions) throws IOException {
		this.extensions = extensions;
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
					for ( String ext : extensions ) {
						String fileWithExtensionPath = f.toString()+"." + ext;
						File f_ext = new File(fileWithExtensionPath);
						if (f_ext.exists() && f_ext.isFile())
							return IRI.create(documentUri+"."+ext);
					}
				}
			}
			return IRI.create(documentUri);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}

}
