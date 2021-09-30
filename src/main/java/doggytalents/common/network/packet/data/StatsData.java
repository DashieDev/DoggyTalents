package doggytalents.common.network.packet.data;

import doggytalents.common.entity.stats.StatsObject;
import doggytalents.common.entity.stats.StatsTracker;

public class StatsData extends DogData {
    public StatsObject stats;
            
    public StatsData(int id, StatsObject stats) {
        super(id);
        this.stats = stats;
    }
}
