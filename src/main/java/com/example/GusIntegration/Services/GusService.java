package com.example.GusIntegration.Services;

import com.example.GusIntegration.Entity.CompanyDTO;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


import org.w3c.dom.Element;
import tools.jackson.databind.ObjectMapper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class GusService {

    public ResponseEntity<?> getAlldata(String type, String registryNumber) {
        final HttpHeaders httpHeaders= new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        String data = "";
        String SessionId = "";
        type = type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
        try {
            SessionId = loginGus();

            if (SessionId != null && type != null && registryNumber != null) {
                data = getData(SessionId,type, registryNumber);

                return new ResponseEntity<String>(data, httpHeaders, HttpStatus.OK);
            }else{
                Exception exception = new Exception("Invalid Data");
                throw exception;
            }

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public String loginGus() throws URISyntaxException {
        String SessionId = null;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(new URI("https://wyszukiwarkaregontest.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc"))
                .POST(HttpRequest.BodyPublishers.ofString("<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:ns=\"http://CIS/BIR/PUBL/2014/07\">"
                        + "<soap:Header xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">"
                        + "<wsa:Action>http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/Zaloguj</wsa:Action>"
                        + "<wsa:To>https://wyszukiwarkaregontest.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc</wsa:To>"
                        + "</soap:Header>"
                        + "<soap:Body>"
                        + "<ns:Zaloguj>"
                        + "<ns:pKluczUzytkownika>abcde12345abcde12345</ns:pKluczUzytkownika>"
                        + "</ns:Zaloguj>"
                        + "</soap:Body>"
                        + "</soap:Envelope>"))
                .header("Content-Type", "application/soap+xml")
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();

            int start = body.indexOf("<ZalogujResult>") + "<ZalogujResult>".length();
            int end = body.indexOf("</ZalogujResult>");

            if (start > 0 && end > start) {
                SessionId = body.substring(start, end);
            }


            if (SessionId != null) {
                return SessionId;
            } else {
                Exception exception = new Exception("Session id is null");
                exception.printStackTrace();

                throw exception;
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
            return null;
        }
    }

    public String getData(String SessionId, String type, String registryNumber) throws URISyntaxException {

        HttpClient client = HttpClient.newHttpClient();
        String dynamicTag = "<dat:" + type + ">" + registryNumber + "</dat:" + type + ">";

        String soapBody = """
                <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                               xmlns:ns="http://CIS/BIR/PUBL/2014/07"
                               xmlns:dat="http://CIS/BIR/PUBL/2014/07/DataContract">
                    <soap:Header xmlns:wsa="http://www.w3.org/2005/08/addressing">
                        <wsa:Action>
                            http://CIS/BIR/PUBL/2014/07/IUslugaBIRzewnPubl/DaneSzukajPodmioty
                        </wsa:Action>
                        <wsa:To>
                            https://wyszukiwarkaregontest.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc
                        </wsa:To>
                    </soap:Header>
                    <soap:Body>
                        <ns:DaneSzukajPodmioty>
                            <ns:pParametryWyszukiwania>
                                %s
                            </ns:pParametryWyszukiwania>
                        </ns:DaneSzukajPodmioty>
                    </soap:Body>
                </soap:Envelope>
                """.formatted(dynamicTag);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://wyszukiwarkaregontest.stat.gov.pl/wsBIR/UslugaBIRzewnPubl.svc"))
                .header("Content-Type", "application/soap+xml")
                .header("sid", SessionId)
                .POST(HttpRequest.BodyPublishers.ofString(soapBody)) // <- tutaj już czysty String
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            /* 1️⃣ Wyciągamy DaneSzukajPodmiotyResult */
            String startTag = "<DaneSzukajPodmiotyResult>";
            String endTag = "</DaneSzukajPodmiotyResult>";

            int start = body.indexOf(startTag) + startTag.length();
            int end = body.indexOf(endTag);

            if (start < 0 || end < 0) {
                System.out.println("Brak danych w odpowiedzi GUS");
                return null;
            }

            String escapedXml = body.substring(start, end);

            String xml = escapedXml
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&#xD;", "")
                    .trim();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            Element dane = (Element) doc.getElementsByTagName("dane").item(0);

            CompanyDTO dto = new CompanyDTO();
            dto.regon = getValue(dane, "Regon");
            dto.nip = getValue(dane, "Nip");
            dto.nazwa = getValue(dane, "Nazwa");
            dto.wojewodztwo = getValue(dane, "Wojewodztwo");
            dto.powiat = getValue(dane, "Powiat");
            dto.gmina = getValue(dane, "Gmina");
            dto.miejscowosc = getValue(dane, "Miejscowosc");
            dto.ulica = getValue(dane, "Ulica");
            dto.nrNieruchomosci = getValue(dane, "NrNieruchomosci");
            dto.kodPocztowy = getValue(dane, "KodPocztowy");

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(dto);

            System.out.println(json);
            return json;

        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.getStackTrace());
            return null;
        }
    }

    private static String getValue(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "";
    }

}
