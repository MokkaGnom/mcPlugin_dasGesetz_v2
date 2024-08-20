package manager.performance;

import manager.ManagedPlugin;

import java.util.*;

@Deprecated
public class PerformanceTracker
{
    public static final PerformanceTracker INSTANCE = new PerformanceTracker();
    public static final String OBJECT_COUNT_FORMAT = "%s: %s";

    public Set<PerformanceTracked> getPerformancePlugins(Set<ManagedPlugin> pluginSet) {
        return new HashSet<>()
        {{
            for(ManagedPlugin plugin : pluginSet) {
                if(plugin instanceof PerformanceTracked pt) {
                    add(pt);
                }
            }
        }};
    }

    public Map<PerformanceTracked, Integer> getObjectCount(Set<PerformanceTracked> pluginSet) {
        return new HashMap<>()
        {{
            for(PerformanceTracked plugin : pluginSet) {
                put(plugin, plugin.getObjectCount());
            }
        }};
    }

    public List<String> getObjectCountAsStringOutput(Set<PerformanceTracked> pluginSet) {
        return new ArrayList<>()
        {{
            for(PerformanceTracked plugin : pluginSet) {
                add(String.format(OBJECT_COUNT_FORMAT, plugin, plugin.getObjectCount()));
            }
        }};
    }
}
