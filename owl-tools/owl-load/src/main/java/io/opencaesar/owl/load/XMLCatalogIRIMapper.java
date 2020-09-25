package io.opencaesar.owl.load;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.xml.catalog.Catalog;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import java.io.File;
import java.util.List;

@SuppressWarnings("serial")
public class XMLCatalogIRIMapper implements OWLOntologyIRIMapper {

	private final File catalogFile;

	private final List<String> extensions;

	public XMLCatalogIRIMapper(File catalogFile, List<String> extensions) {
		this.extensions = extensions;
		if (null == catalogFile || !catalogFile.isFile() || !catalogFile.isAbsolute())
			throw new IllegalArgumentException("The catalogFile must exists and be an absolute path: " + catalogFile);
		this.catalogFile = catalogFile;
	}

	@Nullable
	@Override
	public IRI getDocumentIRI(IRI originalIri) {
		// Due to a bug in the OpenJDK XML Catalog API implementation,
		// we have to create a fresh catalog for each call to `matchURI` otherwise
		// the results may be incorrect.
		// See: https://github.com/NicolasRouquette/xml-catalog-reset-bug/blob/master/README.md
		Catalog catalog = CatalogManager.catalog(CatalogFeatures.builder().build(), catalogFile.toURI());
		try {
			String documentUri = catalog.matchURI(originalIri.toString());
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
