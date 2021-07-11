package io.opencaesar.owl.load;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
				String extension = FilenameUtils.getExtension(documentUri);
				if (extension.isEmpty() || StringUtils.isNumeric(extension)) {
					String documentPath = documentUri.substring(5);
					for ( String ext : extensions ) {
						String uri = (ext.startsWith(".")) ? documentPath+ext : documentPath+"." + ext;
						File f = new File(uri);
						if (f.exists() && f.isFile())
							return IRI.create("file:" + uri);
					}
				}
				return IRI.create(documentUri);
			}
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

}
