package eu.knowledge.engine.smartconnector.runtime.messaging;

import java.net.URI;
import java.net.URISyntaxException;

public class Utils {
  public static URI stripUserInfoFromURI(URI u) {
    try {
      return new URI(u.getScheme(),null, u.getHost(), u.getPort(),u.getPath(), u.getQuery(), u.getFragment());
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid URI.");
    }
  }
}
