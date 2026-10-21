package org.integratedmodelling.klab.authority.land;

import java.util.List;
import org.integratedmodelling.klab.api.services.Authority;
import org.integratedmodelling.klab.api.services.runtime.Notification;

/** Nested patterns are arguments, not superclass edges. */
record LandIdentity(String id, String conceptName, String authorityName, String baseIdentity,
                    String description, String label, String locator, List<Notification> notifications, List<String> parentIds)
    implements Authority.Identity {
  LandIdentity(String id, String conceptName, String authorityName, String baseIdentity,
      String description, String label, String locator, List<Notification> notifications) {
    this(id,conceptName,authorityName,baseIdentity,description,label,locator,notifications,List.of());
  }
  LandIdentity { notifications = List.copyOf(notifications); parentIds = List.copyOf(parentIds); }
  @Override public String getId() { return id; }
  @Override public String getConceptName() { return conceptName; }
  @Override public String getAuthorityName() { return authorityName; }
  @Override public String getBaseIdentity() { return baseIdentity; }
  @Override public List<String> getParentIds() { return parentIds; }
  @Override public List<String> getParentRelationship() { return List.of(); }
  @Override public String getDescription() { return description; }
  @Override public String getLabel() { return label; }
  @Override public float getScore() { return notifications.isEmpty() ? 1 : 0; }
  @Override public String getLocator() { return locator; }
  @Override public List<Notification> getNotifications() { return notifications; }
}
