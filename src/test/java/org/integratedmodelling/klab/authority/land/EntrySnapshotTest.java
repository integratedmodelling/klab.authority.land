package org.integratedmodelling.klab.authority.land;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.*;
import org.integratedmodelling.klab.api.services.*;
import org.junit.jupiter.api.Test;

class EntrySnapshotTest {
  private Authority.ConfigurationRequest request() {
    return new Authority.ConfigurationRequest("w","LAND","land:CoverIdentity",
        Map.of("urn",LandAuthority.URN,"mode","ENTRY_ONLY","domain","LULC","catalogs",List.of("igbp_modis61_draft")));
  }
  private Authority.ConfigurationRequest changed(String worldview,String name,String root,Map<String,Object> parameters) {
    return new Authority.ConfigurationRequest(worldview,name,root,parameters);
  }
  @Test void explicitEntryBridgeWorksWithoutPatternSemanticsAndKeepsSixSevenDistinct() {
    try(var a=new LandAuthority()) {
      var id=a.configure(request());
      var six=a.reconcile(id,Map.of("code","6"));var seven=a.reconcile(id,Map.of("code","7"));
      assertTrue(six.getNotifications().isEmpty());assertTrue(seven.getNotifications().isEmpty());
      assertNotEquals(six.getId(),seven.getId());assertEquals("land:CoverIdentity",six.getBaseIdentity());
      assertEquals(0,a.semanticDistance(id,six.getId(),six.getId()).distance());
      assertEquals(Authority.DistanceStatus.UNKNOWN,a.semanticDistance(id,six.getId(),seven.getId()).status());
      assertEquals(MediationResult.Status.UNKNOWN,a.mediate(id,six.getId(),seven.getId()).status());
      assertFalse(a.resolveIdentity(id,"physical:HorizontalArrangement(land:Tree)").getNotifications().isEmpty());
      assertThrows(IllegalStateException.class,()->a.validatePattern(id,"physical:HorizontalArrangement(land:Tree)"));
    }
  }
  @Test void deterministicFrozenReplayUsesNoMutableRegisteredCatalogAndSurvivesProducerClose() {
    var request=request();var producer=new LandAuthority();var id=producer.configure(request);
    var snapshot=producer.captureConfiguration(id);
    assertEquals(snapshot,producer.captureConfiguration(producer.configure(request)));
    var six=producer.reconcile(id,Map.of("code","6"));
    var descriptions=producer.getCodelists(id).get("igbp_modis61_draft").getDescription();
    producer.close();
    try(var restored=new LandAuthority()) {
      var replay=restored.restoreConfiguration(request,snapshot);
      assertEquals(six.getId(),restored.reconcile(replay,Map.of("code","6")).getId());
      assertEquals(six.getDescription(),restored.resolveIdentity(replay,six.getId()).getDescription());
      assertEquals(descriptions,restored.getCodelists(replay).get("igbp_modis61_draft").getDescription());
      assertFalse(restored.search("6",null,replay).isEmpty());
      assertEquals(snapshot,restored.captureConfiguration(replay));
      restored.releaseConfiguration(replay);
      assertThrows(IllegalArgumentException.class,()->restored.getCodelists(replay));
    }
  }
  @Test void snapshotPayloadCannotBeRewrappedUnderAnotherBinding() {
    try(var a=new LandAuthority()) {
      var snapshot=a.captureConfiguration(a.configure(request()));
      for(var changed:List.of(changed("other","LAND",request().rootIdentity(),request().parameters()),
          changed("w","OTHER",request().rootIdentity(),request().parameters()),
          changed("w","LAND","land:OtherIdentity",request().parameters()))) {
        assertThrows(IllegalArgumentException.class,()->a.restoreConfiguration(changed,snapshot));
        var rewrapped=new ConfiguredAuthoritySnapshot(snapshot.format(),LandAuthority.URN,changed,snapshot.state());
        assertThrows(IllegalArgumentException.class,()->a.restoreConfiguration(changed,rewrapped));
      }
    }
  }
  @Test void malformedUnknownDuplicateAndOversizedSnapshotsFailWithoutReplacingGoodBridge() {
    try(var a=new LandAuthority()) {
      var id=a.configure(request());var good=a.captureConfiguration(id);
      var text=new String(good.state(),StandardCharsets.UTF_8);
      for(var malformed:List.of("{}",text+" {}",text.replaceFirst("\\{","{\"unknown\":1,"),
          text.replaceFirst("\\{","{\"request\":null,")," "+text)) {
        var bad=new ConfiguredAuthoritySnapshot(good.format(),LandAuthority.URN,request(),malformed.getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class,()->a.restoreConfiguration(request(),bad));
      }
      var oversized=new ConfiguredAuthoritySnapshot(good.format(),LandAuthority.URN,request(),new byte[EntrySnapshotCodec.MAX_BYTES+1]);
      assertThrows(IllegalArgumentException.class,()->a.restoreConfiguration(request(),oversized));
      assertTrue(a.reconcile(id,Map.of("code","6")).getNotifications().isEmpty());
      assertThrows(IllegalArgumentException.class,()->a.restoreConfiguration(request(),
          new ConfiguredAuthoritySnapshot("unknown-v1",LandAuthority.URN,request(),good.state())));
    }
  }
  @Test void frozenRevisionAndEditionRemainExactEvenWithDifferentLocalRegistry() {
    var first=new LandAuthority();var second=new LandAuthority();
    var entry=new PatternCatalog.Entry("Cover","Cover",null,"006",null,List.of());
    var old=new PatternCatalog("private_catalog",PatternCatalog.Domain.LULC,"r1",URI.create("https://example.org"),List.of(entry),
        new PatternCatalog.Metadata("Private","edition1",false,List.of("note"),true));
    first.registerCatalog(old);
    second.registerCatalog(new PatternCatalog(old.id(),old.domain(),"r2",old.source(),old.entries(),
        new PatternCatalog.Metadata("Private","edition2",false,List.of(),false)));
    var parameters=new HashMap<String,Object>(request().parameters());parameters.put("catalogs",List.of(old.id()));
    var request=changed("w","LAND",request().rootIdentity(),parameters);
    var frozen=first.captureConfiguration(first.configure(request));first.close();
    var id=second.restoreConfiguration(request,frozen);
    assertEquals("r1",CatalogEntryId.decode(second.reconcile(id,Map.of("code","006")).getId()).revision());
    assertTrue(second.getCodelistDefinitions(id).get(old.id()).acceptsProposals());
    assertFalse(second.reconcile(id,Map.of("code","6")).getNotifications().isEmpty());
    second.close();
  }
  @Test void callerMutationAndLifecycleCannotChangeCapturedRequestOrResurrectReleasedBridges() {
    var a=new LandAuthority();var selected=new ArrayList<>(List.of("igbp_modis61_draft"));
    var parameters=new HashMap<String,Object>(request().parameters());parameters.put("catalogs",selected);
    var request=changed("w","LAND",request().rootIdentity(),parameters);var id=a.configure(request);selected.clear();
    assertEquals(List.of("igbp_modis61_draft"),a.captureConfiguration(id).request().parameters().get("catalogs"));
    a.unbindWorldview("w");assertThrows(IllegalArgumentException.class,()->a.captureConfiguration(id));
    a.close();assertThrows(IllegalStateException.class,()->a.configure(request()));
  }
  @Test void defaultModeAndUnsupportedParametersDoNotGainEntryOnlyPrivileges() {
    try(var a=new LandAuthority()) {
      var parameters=new HashMap<String,Object>(request().parameters());parameters.remove("mode");
      assertThrows(IllegalStateException.class,()->a.configure(changed("w","LAND",request().rootIdentity(),parameters)));
      parameters.put("mode","TYPO");assertThrows(IllegalArgumentException.class,()->a.configure(changed("w","LAND",request().rootIdentity(),parameters)));
      parameters.put("mode","ENTRY_ONLY");parameters.put("mediation",Map.of());
      assertThrows(IllegalArgumentException.class,()->a.configure(changed("w","LAND",request().rootIdentity(),parameters)));
    }
  }
  @Test void allBundledCatalogsIncludingTypedAnnotationsRoundTripInTheirDeclaredDomains() {
    try(var producer=new LandAuthority();var restored=new LandAuthority()) {
      for(var catalog:producer.getCatalogs().values()) {
        var parameters=new HashMap<String,Object>(request().parameters());
        parameters.put("domain",catalog.domain().name());parameters.put("catalogs",List.of(catalog.id()));
        var request=changed("w","LAND",request().rootIdentity(),parameters);
        var original=producer.configure(request);var snapshot=producer.captureConfiguration(original);
        var replay=restored.restoreConfiguration(request,snapshot);
        assertEquals(snapshot,restored.captureConfiguration(replay));
        assertEquals(producer.getCodelists(original).get(catalog.id()).size(),
            restored.getCodelists(replay).get(catalog.id()).size());
      }
    }
  }
  @Test void reviewedAnnotationsCannotNormalizeIntoDifferentSnapshotContentOnRestore() {
    try(var provider=new LandAuthority()) {
      var entry=new PatternCatalog.Entry("Cover","Cover","physical:HorizontalArrangement(land:Tree)","6",null,List.of(),null,
          PatternCatalog.EntryVerification.VERIFIED,PatternCatalog.DefinitionReview.REVIEWED,List.of());
      provider.registerCatalog(new PatternCatalog("reviewed",PatternCatalog.Domain.LULC,"r1",URI.create("https://example.org"),List.of(entry)));
      var parameters=new HashMap<String,Object>(request().parameters());parameters.put("catalogs",List.of("reviewed"));
      var request=changed("w","LAND",request().rootIdentity(),parameters);
      var snapshot=provider.captureConfiguration(provider.configure(request));
      var changed=new String(snapshot.state(),StandardCharsets.UTF_8).replace("physical:HorizontalArrangement(land:Tree)","physical:HorizontalArrangement( land:Tree )");
      assertNotEquals(new String(snapshot.state(),StandardCharsets.UTF_8),changed);
      var malformed=new ConfiguredAuthoritySnapshot(snapshot.format(),snapshot.providerUrn(),request,changed.getBytes(StandardCharsets.UTF_8));
      assertThrows(IllegalArgumentException.class,()->provider.restoreConfiguration(request,malformed));
    }
  }
  @Test void concurrentCaptureAndCloseNeverExportAPartiallyReleasedBridge() throws Exception {
    var provider=new LandAuthority();var request=request();var id=provider.configure(request);
    try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var start=new java.util.concurrent.CountDownLatch(1);
      var capturing=executor.submit(()->{start.await();try{return provider.captureConfiguration(id);}
        catch(IllegalArgumentException released){return null;}});
      var closing=executor.submit(()->{start.await();provider.close();return true;});
      start.countDown();var snapshot=capturing.get(10,java.util.concurrent.TimeUnit.SECONDS);
      closing.get(10,java.util.concurrent.TimeUnit.SECONDS);
      if(snapshot!=null)try(var restored=new LandAuthority()) {
        assertFalse(restored.getCodelists(restored.restoreConfiguration(request,snapshot)).isEmpty());
      }
      assertThrows(IllegalArgumentException.class,()->provider.captureConfiguration(id));
    }
  }
}

