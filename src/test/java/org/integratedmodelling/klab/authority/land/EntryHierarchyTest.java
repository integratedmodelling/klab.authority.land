package org.integratedmodelling.klab.authority.land;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.integratedmodelling.klab.api.services.*;
import org.junit.jupiter.api.Test;

class EntryHierarchyTest {
  static PatternCatalog.Entry entry(String code, String... parents) {
    return new PatternCatalog.Entry(code,code,null,code,URI.create("https://example.org/synthetic"),
        List.of("Synthetic class-inclusion fixture, not a real typology"),null,
        PatternCatalog.EntryVerification.UNVERIFIED,PatternCatalog.DefinitionReview.ABSENT,
        List.of(),List.of(parents));
  }
  static PatternCatalog catalog(String id, PatternCatalog.Entry... entries) {
    return new PatternCatalog(id,PatternCatalog.Domain.LULC,"r1",URI.create("https://example.org/synthetic"),
        List.of(entries),new PatternCatalog.Metadata("Synthetic","edition1",true,List.of(),false));
  }
  static Authority.ConfigurationRequest request(String... catalogs) {
    return new Authority.ConfigurationRequest("fixture","LAND","land:LandCover",Map.of(
        "urn",LandAuthority.URN,"mode","ENTRY_ONLY","domain","LULC","catalogs",List.of(catalogs)));
  }
  static String code(LandAuthority provider,String id,String source) {
    var result=provider.reconcile(id,Map.of("code",source));assertTrue(result.getNotifications().isEmpty());return result.getId();
  }
  @Test void explicitHierarchyDelegatesStrictPositiveRefinementWithoutPatterns() {
    try(var provider=new LandAuthority()) {
      provider.registerCatalog(catalog("synthetic",entry("Vegetation"),entry("Forest","Vegetation"),
          entry("Seasonal"),entry("DeciduousForest","Forest","Seasonal"),entry("Water")));
      var id=provider.configure(request("synthetic"));
      var leaf=code(provider,id,"DeciduousForest");var forest=code(provider,id,"Forest");var root=code(provider,id,"Vegetation");
      assertEquals(0,provider.semanticDistance(id,leaf,leaf).distance());
      for(var parent:List.of(forest,root,code(provider,id,"Seasonal"))) {
        var distance=provider.semanticDistance(id,parent,leaf);
        assertEquals(Authority.DistanceStatus.COMPATIBLE,distance.status());assertEquals(1,distance.distance());
        assertEquals(Authority.DistanceStatus.UNKNOWN,provider.semanticDistance(id,leaf,parent).status());
      }
      assertEquals(2,provider.resolveIdentity(id,leaf).getParentIds().size());
      assertEquals(Authority.DistanceStatus.UNKNOWN,provider.semanticDistance(id,code(provider,id,"Water"),leaf).status());
      assertThrows(IllegalStateException.class,()->provider.validatePattern(id,"physical:HorizontalArrangement(land:Tree)"));
      assertEquals(MediationResult.Status.UNKNOWN,provider.mediate(id,forest,leaf).status());
    }
  }
  @Test void corineSourceHierarchyKeepsPinnedParentIdentityAndUnrelatedNegatives() {
    try(var provider=new LandAuthority()) {
      var id=provider.configure(request("corine_entry_hierarchy_example"));
      var leaf=code(provider,id,"311");var parent=code(provider,id,"31");var ancestor=code(provider,id,"3");
      assertEquals(List.of(parent),provider.resolveIdentity(id,leaf).getParentIds());
      assertEquals(1,provider.semanticDistance(id,ancestor,leaf).distance());
      assertEquals(Authority.DistanceStatus.UNKNOWN,provider.semanticDistance(id,leaf,parent).status());
      assertEquals(Authority.DistanceStatus.UNKNOWN,provider.semanticDistance(id,code(provider,id,"111"),leaf).status());
      assertEquals("illustrative-1",CatalogEntryId.decode(parent).revision());
      assertEquals(CatalogEntryId.decode(leaf).sourceEdition(),CatalogEntryId.decode(parent).sourceEdition());
      assertTrue(provider.reconcile(id,Map.of("code","0311")).getNotifications().stream().anyMatch(n->n.getLevel().severity>=3));
      assertTrue(provider.reconcile(id,Map.of("code","999")).getNotifications().stream().anyMatch(n->n.getLevel().severity>=3));
      assertEquals(8,provider.getCodelists(id).get("corine_entry_hierarchy_example").size());
    }
  }
  @Test void equalLabelsAndCodesDoNotCrossPinnedCatalogOrEditionBoundaries() {
    try(var provider=new LandAuthority()) {
      var one=catalog("one",entry("Forest"),entry("DeciduousForest","Forest"));
      var two=catalog("two",entry("Forest"),entry("DeciduousForest","Forest"));
      provider.registerCatalog(one);provider.registerCatalog(two);var id=provider.configure(request("one","two"));
      assertEquals(Authority.DistanceStatus.UNKNOWN,provider.semanticDistance(id,one.identityCode(one.entries().getFirst()),
          two.identityCode(two.entries().getLast())).status());
      var wrong=new CatalogEntryId(one.id(),"r2",one.metadata().sourceVersion(),"Forest").code();
      assertEquals(Authority.DistanceStatus.UNKNOWN,provider.semanticDistance(id,wrong,one.identityCode(one.entries().getLast())).status());
    }
  }
  @Test void unknownParentsSelfLinksDuplicatesAndCyclesAreRejectedBeforeConfiguration() {
    assertThrows(IllegalArgumentException.class,()->catalog("missing",entry("Leaf","Missing")));
    assertThrows(IllegalArgumentException.class,()->entry("Leaf","Leaf"));
    assertThrows(IllegalArgumentException.class,()->entry("Leaf","Parent","Parent"));
    assertThrows(IllegalArgumentException.class,()->catalog("cycle",entry("One","Two"),entry("Two","One")));
  }
  @Test void hierarchyAndEntryMetadataSurviveTransportAndOfflineReplay() throws Exception {
    var producer=new LandAuthority();var request=request("corine_entry_hierarchy_example");var id=producer.configure(request);
    var leaf=code(producer,id,"311");var parent=code(producer,id,"31");var snapshot=producer.captureConfiguration(id);
    var json=org.integratedmodelling.common.data.jackson.JacksonConfiguration.newObjectMapper();
    var copy=json.readValue(json.writeValueAsBytes(snapshot),ConfiguredAuthoritySnapshot.class);
    producer.close();
    try(var restored=new LandAuthority()) {
      var replay=restored.restoreConfiguration(request,copy);
      assertEquals(List.of(parent),restored.resolveIdentity(replay,leaf).getParentIds());
      assertEquals(1,restored.semanticDistance(replay,parent,leaf).distance());
      assertEquals(leaf,restored.getCodelists(replay).get("corine_entry_hierarchy_example").value("BroadLeavedForest"));
      assertThrows(IllegalArgumentException.class,()->restored.restoreConfiguration(
          new Authority.ConfigurationRequest("other","LAND",request.rootIdentity(),request.parameters()),copy));
    }
  }
  @Test void unsupportedOldSnapshotFormatIsRejectedWithoutIgnoringHierarchy() {
    var request=request("corine_entry_hierarchy_example");
    try(var provider=new LandAuthority()) {
      var snapshot=provider.captureConfiguration(provider.configure(request));
      var old=new ConfiguredAuthoritySnapshot("land.catalog-entries.v1",LandAuthority.URN,request,snapshot.state());
      assertThrows(IllegalArgumentException.class,()->provider.restoreConfiguration(request,old));
    }
  }
}
