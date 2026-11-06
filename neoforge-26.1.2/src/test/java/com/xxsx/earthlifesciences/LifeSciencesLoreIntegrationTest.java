package com.xxsx.earthlifesciences;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xxsx.earthlore.api.Grade;
import com.xxsx.earthlore.core.Gate;
import com.xxsx.earthlore.core.GraphValidator;
import com.xxsx.earthlore.core.NodeDef;
import com.xxsx.earthlore.core.Prereq;
import com.xxsx.earthlore.core.Reachability;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LifeSciencesLoreIntegrationTest {
    private static final Set<String> DISCIPLINES = Set.of(
            "earth_lore:geo", "earth_lore:che", "earth_lore:mat",
            "earth_lore:eng", "earth_lore:agr", "earth_lore:med",
            "earth_lore:bio", "earth_lore:phy", "earth_lore:cmp"
    );
    private static final GraphValidator.DependencyRule DEPS = (owner, other) ->
            owner.equals(other) || "earth_lore".equals(other)
                    || ("earth_life_sciences".equals(owner) && "earth_on_minecraft".equals(other));

    @Test
    void allThirtyEightLifeScienceNodesAndEomNodesValidateAndAreReachableWithoutPhysicalSciences() throws IOException {
        List<NodeDef> lifeNodes = loadNodes(
                Path.of("src/main/resources/data/earth_life_sciences/earth_lore/knowledge_node"),
                "earth_life_sciences"
        );
        assertEquals(38, lifeNodes.size(), "Expected 17 BIO + 21 MED = 38 nodes");

        List<NodeDef> eomNodes = loadNodes(
                Path.of("../../earth_on_minecraft/neoforge-26.2/src/main/resources/data/earth_on_minecraft/earth_lore/knowledge_node"),
                "earth_on_minecraft"
        );
        List<NodeDef> combined = new ArrayList<>();
        combined.addAll(eomNodes);
        combined.addAll(lifeNodes);

        GraphValidator.Result validated = GraphValidator.validate(combined, DISCIPLINES, DEPS);
        assertTrue(validated.rejected().isEmpty(), () -> "Rejected nodes: " + validated.rejected());
        assertEquals(combined.size(), validated.accepted().size());

        Reachability.Report reach = Reachability.compute(validated.accepted());
        assertTrue(reach.unreachable().isEmpty(), () -> "Unreachable nodes: " + reach.unreachable());
        assertEquals(combined.size(), reach.learnable().size());
        assertTrue(reach.altUsed() >= 3, "K-BIO-13, K-MED-30, K-MED-32 should use self-taught fallbacks without earth_physical_sciences");
    }

    @Test
    void microscopeCultureFermenterCentrifugeBreedingGeneEditAndSurgeryMatchSpec() {
        // earth_life_microscope_readout
        assertEquals("has_microbe", LifeScienceMath.formatMicroscopeReadout("penicillium", 0.85D, false));
        assertEquals("penicillium:0.85", LifeScienceMath.formatMicroscopeReadout("penicillium", 0.85D, true));

        // earth_life_culture_pure: sterile dish (+0.10) + fine medium (+0.10) + 0.05 = 0.80 >= 0.80
        double pureVal = LifeScienceMath.computeCulturePurity(true, Grade.FINE, 0.05D);
        assertTrue(pureVal >= 0.80D);
        assertTrue(LifeScienceMath.isCulturePure(pureVal));

        // earth_life_culture_contaminated: unsterile dish + crude medium + 0.02 = 0.57 < 0.80
        double contamVal = LifeScienceMath.computeCulturePurity(false, Grade.CRUDE, 0.02D);
        assertFalse(LifeScienceMath.isCulturePure(contamVal));

        // Subculture exhaustion at generation 32
        assertEquals("strain_exhausted", LifeScienceMath.computeSubculture(0.90D, 32, true).fault());

        // earth_life_fermenter_penicillin & earth_life_soft_cap_penicillin
        LifeScienceMath.FermentBatchResult pureBatch = LifeScienceMath.computeFermentBatch(0.85D);
        assertEquals(4000, pureBatch.brothMb());
        assertFalse(pureBatch.contaminated());
        LifeScienceMath.FermentBatchResult impureBatch = LifeScienceMath.computeFermentBatch(0.65D);
        assertEquals(1200, impureBatch.brothMb());
        assertTrue(impureBatch.contaminated());

        double drugPurity = LifeScienceMath.computeCentrifugeDrugPurity(0.90D, true);
        assertTrue(drugPurity >= 0.83D);
        assertEquals(Grade.COMMON, LifeScienceMath.resolveCentrifugeDrugGrade(0.90D, false, false));
        assertEquals(Grade.FINE, LifeScienceMath.resolveCentrifugeDrugGrade(0.90D, true, false));

        // earth_life_breeding_cross
        LifeComponents.GenotypeComponent parentA = new LifeComponents.GenotypeComponent(true, 4, 6, 8, 2, 10, 1);
        LifeComponents.GenotypeComponent parentB = new LifeComponents.GenotypeComponent(true, 8, 2, 4, 6, 10, 2);
        LifeComponents.GenotypeComponent child = LifeScienceMath.crossGenotypes(
                parentA, parentB, new int[]{0, 1, 2, 2, 0}, 0.01D, 4, 1);
        assertEquals(4, child.yield());
        assertEquals(2, child.tolerance());
        assertEquals(6, child.potency());
        assertEquals(4, child.growth());
        assertEquals(10, child.hardiness(), "Mutation at 10 + 1 must clamp to 10");
        assertEquals(3, child.generation());

        // Gene edit with off-target
        LifeScienceMath.GeneEditResult edited = LifeScienceMath.applyGeneEdit(parentA, "yield", 9, 0.02D, 1);
        assertEquals(9, edited.genotype().yield());
        assertEquals(5, edited.genotype().tolerance(), "Off-target should reduce trait 1 by 1");
        assertTrue(edited.offTarget());
        assertEquals("off_target", edited.fault());

        // earth_life_surgery_sterile & earth_life_surgery_unsterile
        assertEquals("lore_locked", LifeScienceMath.evaluateSurgery(false, false, true, true, true, true, "torso").failCode());
        assertEquals("self_surgery", LifeScienceMath.evaluateSurgery(true, true, true, true, true, true, "torso").failCode());
        LifeScienceMath.SurgeryOutcome sterileSurg = LifeScienceMath.evaluateSurgery(true, false, true, true, true, true, "torso");
        assertTrue(sterileSurg.success());
        assertFalse(sterileSurg.infected());
        LifeScienceMath.SurgeryOutcome unsterileSurg = LifeScienceMath.evaluateSurgery(true, false, true, true, true, false, "torso");
        assertTrue(unsterileSurg.success());
        assertTrue(unsterileSurg.infected());
    }

    private static List<NodeDef> loadNodes(Path root, String ownerNamespace) throws IOException {
        List<NodeDef> list = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String rel = root.relativize(file).toString().replace('\\', '/').replaceAll("\\.json$", "");
                String id = ownerNamespace + ":" + rel;
                JsonObject obj = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                String discipline = obj.get("discipline").getAsString();
                int tier = obj.get("tier").getAsInt();
                Gate gate = Gate.parse(obj.get("gate").getAsString()).orElseThrow();
                List<Prereq> requires = new ArrayList<>();
                for (JsonElement reqEl : obj.getAsJsonArray("requires")) {
                    JsonObject reqObj = reqEl.getAsJsonObject();
                    if (reqObj.has("all")) {
                        requires.add(new Prereq.All(reqObj.get("all").getAsString()));
                    } else if (reqObj.has("any")) {
                        List<String> members = new ArrayList<>();
                        JsonArray arr = reqObj.getAsJsonArray("any");
                        arr.forEach(m -> members.add(m.getAsString()));
                        requires.add(new Prereq.Any(members));
                    }
                }
                List<String> unlockKeys = new ArrayList<>();
                for (JsonElement uEl : obj.getAsJsonArray("unlocks")) {
                    JsonObject uObj = uEl.getAsJsonObject();
                    Map.Entry<String, JsonElement> entry = uObj.entrySet().iterator().next();
                    String kind = entry.getKey();
                    String val = entry.getValue().getAsString();
                    unlockKeys.add("capstone".equals(kind) ? ("capstone:" + val + "@" + ownerNamespace) : (kind + ":" + val));
                }
                list.add(new NodeDef(id, discipline, tier, gate, requires, unlockKeys, false));
            }
        }
        return list;
    }
}
