package net.fabric_extras.structure_pool.api;

import java.util.ArrayList;
import java.util.List;

public class StructurePoolConfig {
    public ArrayList<Entry> entries = new ArrayList<>();
    public static class Entry { public Entry() { }
        public String pool;

        public ArrayList<Structure> structures = new ArrayList<>();

        public static class Structure { public Structure() { }
            public String id;
            public int weight = 1;
            public int limit = 0;


            public Structure(String id, int weight) {
                this(id, weight, 0);
            }

            public Structure(String id, int weight, int limit) {
                this.id = id;
                this.weight = weight;
                this.limit = limit;
            }
        }

        public Entry(String poolId, String structureId, int weight) {
            this(poolId, new ArrayList<Structure>(List.of(new Structure(structureId, weight))));
        }

        public Entry(String poolId, String structureId, int weight, int limit) {
            this(poolId, new ArrayList<Structure>(List.of(new Structure(structureId, weight, limit))));
        }

        public Entry(String pool, ArrayList<Structure> structures) {
            this.pool = pool;
            this.structures = structures;
        }
    }
}
