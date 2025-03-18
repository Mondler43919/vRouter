package vRouter;

//缓存增加
public class WeightCalculator {

    // 权重系数
    private double w1;  // Frequency的权重
    private double w2;  // Distance的权重
    private double w3;  // Activity score的权重

    // 构造函数，用于初始化权重系数
    public WeightCalculator(double w1, double w2, double w3) {
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
    }

    // 计算优先级
    public double calculatePriority(double frequency, double distance, double activityScore) {
        return w1 * frequency + w2 * distance + w3 * activityScore;
    }
}

