package manager.performance;

@Deprecated
public interface PerformanceTracked
{
    default int getObjectCount(){
        return 1;
    }
}
