INSERT INTO pdf_properties (property_name, property_enabled)
VALUES
  ('pdfVersion', 1),
  ('flavour', 1),
  ('producer', 1),
  ('processingTime', 1),
  ('veraPDFVersion', 1)
;

INSERT INTO pdf_properties_xpath (property_name, xpath_index, xpath)
VALUES
  ('pdfVersion', 0, '/report/jobs/job/featuresReport/lowLevelInfo/pdfVersion'),
  ('flavour', 0, '/report/jobs/job/validationReport/@profileName'),
  ('producer', 0, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/@pdf:Producer'),
  ('producer', 1, '/report/jobs/job/featuresReport/metadata/xmpPackage/x:xmpmeta/rdf:RDF/rdf:Description/pdf:Producer'),
  ('producer', 2, '/report/jobs/job/featuresReport/informationDict/entry[@key=''Producer'']'),
  ('processingTime', 0, '/report/jobs/job/duration'),
  ('veraPDFVersion', 0, '/report/buildInformation/releaseDetails[@id=''gui'']/@version')
;

INSERT INTO pdf_properties_namespaces (namespace_prefix, namespace_url)
VALUES
  ('x', 'adobe:ns:meta/'),
  ('rdf', 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'),
  ('pdf', 'http://ns.adobe.com/pdf/1.3/')
;