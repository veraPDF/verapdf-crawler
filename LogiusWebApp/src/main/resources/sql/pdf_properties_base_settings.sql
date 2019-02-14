INSERT INTO pdf_properties (property_name, property_enabled)
VALUES
  ('flavourPart', TRUE),
  ('flavourConformance', TRUE),
  ('modDateXMP', TRUE),
  ('modDateInfoDict', TRUE),
  ('pdfVersion', TRUE),
  ('producer', TRUE),
  ('processingTime', TRUE),
  ('veraPDFVersion', TRUE)
;

INSERT INTO pdf_properties_xpath (property_name, xpath_index, xpath)
VALUES
  ('flavourPart', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdfaid:part'),
  ('flavourPart', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdfaid:part'),
  ('PDF/E', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdfe:ISO_PDFEVersion'),
  ('PDF/E', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdfe:ISO_PDFEVersion'),
  ('PDF/X', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdfxid:GTS_PDFXVersion'),
  ('PDF/X', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdfxid:GTS_PDFXVersion'),
  ('PDF/UA', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdfuaid:part'),
  ('PDF/UA', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdfuaid:part'),
  ('flavourConformance', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdfaid:conformance'),
  ('flavourConformance', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdfaid:conformance'),
  ('modDateXMP', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@xmp:ModifyDate'),
  ('modDateXMP', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/xmp:ModifyDate'),
  ('modDateInfoDict', 0, '/report/jobs/job/featuresReport/informationDict/entry[@key=''ModDate'']'),
  ('pdfVersion', 0, '/report/jobs/job/featuresReport/lowLevelInfo/pdfVersion'),
  ('producer', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdf:Producer'),
  ('producer', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdf:Producer'),
  ('producer', 2, '/report/jobs/job/featuresReport/informationDict/entry[@key=''Producer'']'),
  ('processingTime', 0, '/report/jobs/job/duration'),
  ('veraPDFVersion', 0, '/report/buildInformation/releaseDetails[@id=''gui'']/@version')
;

INSERT INTO pdf_properties_namespaces (namespace_prefix, namespace_url)
VALUES
  ('x', 'adobe:ns:meta/'),
  ('xmp', 'http://ns.adobe.com/xap/1.0/'),
  ('pdf', 'http://ns.adobe.com/pdf/1.3/'),
  ('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'),
  ('pdfe', 'http://www.aiim.org/pdfe/ns/id/'),
  ('pdfaid', 'http://www.aiim.org/pdfa/ns/id/'),
  ('pdfxid', 'http://www.npes.org/pdfx/ns/id/'),
  ('pdfuaid', 'http://www.aiim.org/pdfua/ns/id/')
;