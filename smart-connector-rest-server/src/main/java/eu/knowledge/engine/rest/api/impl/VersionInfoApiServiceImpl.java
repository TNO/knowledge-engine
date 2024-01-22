package eu.knowledge.engine.rest.api.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Response.Status;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.knowledge.engine.rest.api.NotFoundException;
import eu.knowledge.engine.rest.api.VersionInfoApiService;
import eu.knowledge.engine.rest.model.VersionInfo;

public class VersionInfoApiServiceImpl extends VersionInfoApiService {
	private static final Logger LOG = LoggerFactory.getLogger(VersionInfoApiServiceImpl.class);

	private static final String POM_RESOURCE_PATH = "/META-INF/maven/eu.knowledge.engine/smart-connector-rest-server/pom.xml";
	private static final String POM_LOCAL_PATH = "pom.xml";

	private VersionInfo versionInfo;

	@Override
	public Response versionGet(SecurityContext securityContext) throws NotFoundException {
		if (versionInfo == null) {
			this.setVersionInfoFromFile();
		}
		return Response.status(Status.OK).entity(versionInfo).build();
	}

	private void setVersionInfoFromFile() {
		try {
			MavenXpp3Reader reader = new MavenXpp3Reader();
			LOG.info("Trying to read version info from {}.", POM_RESOURCE_PATH);

			if ((new File(POM_LOCAL_PATH)).exists()) {
				LOG.info("Reading POM from {}", POM_LOCAL_PATH);
				Model model = reader.read(new FileReader(POM_LOCAL_PATH));
				// If we're reading from POM_LOCAL_PATH, the $revision variable is not
				// yet in the version field, so we have to read from the property
				// directly..
				this.versionInfo = new VersionInfo().version(model.getProperties().getProperty("revision"));
				LOG.info("Successfully read version info (via 'revision' property) from {}.", POM_LOCAL_PATH);
			} else {
				LOG.info("Reading POM from {}", POM_RESOURCE_PATH);
				Model model = reader.read(
						new InputStreamReader(VersionInfoApiServiceImpl.class.getResourceAsStream(POM_RESOURCE_PATH)));
				this.versionInfo = new VersionInfo().version(model.getVersion());
				LOG.info("Successfully read version info from {}.", POM_RESOURCE_PATH);
			}
		} catch (IOException | XmlPullParserException e) {
			LOG.error("Could not read version info from {} or {}: {}", POM_LOCAL_PATH, POM_RESOURCE_PATH, e);
		}

		if (this.versionInfo.getVersion() == null) {
			this.versionInfo.setVersion("");
		}
	}
}
