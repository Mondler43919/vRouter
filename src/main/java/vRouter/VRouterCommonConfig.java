package vRouter;

public class VRouterCommonConfig {


    //Bloom Filter Configuration
    public static int EXPECTED_ELEMENTS = 1000;
    public static double FALSE_POSITIVE_PROB = 0.01;

    /**
     * short information about current mspastry configuration
     *
     * @return String
     */
    public static String info() {
        return String.format("[EXPECTED_ELEMENTS=%d][FALSE_POSITIVE_PROB=%f]", EXPECTED_ELEMENTS,FALSE_POSITIVE_PROB);
    }
}
