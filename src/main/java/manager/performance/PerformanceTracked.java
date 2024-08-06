package manager.performance;

public interface PerformanceTracked
{
    default int getObjectCount(){
        return 1;
    }
}
