?building <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/Building> . 
?building <https://saref.etsi.org/saref4bldg/hasSpace> ?buildingSpace . 
?buildingSpace <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/saref4bldg/BuildingSpace> . 
?buildingSpace <https://saref.etsi.org/saref4bldg/contains> ?device . 
?device <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://saref.etsi.org/core/Device> . 
?device <https://www.example.org/interconnect/hasClassification> ?classification . 
?device <http://www.w3.org/2000/01/rdf-schema#comment> ?description . 
?device <https://www.example.org/interconnect/hasServiceProviderEMSOwner> ?serviceProviderEMSOwner .