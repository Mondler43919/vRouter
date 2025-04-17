package vRouter;

import java.util.List;

public class MerkleVerifier {
    public static boolean verify(AccessRecord record, List<String> merklePath, String merkleRoot) {
        String currentHash = HashUtils.SHA256(record.toHashString());

        for (String siblingHash : merklePath) {
            currentHash = HashUtils.SHA256(currentHash + siblingHash);
        }

        return currentHash.equals(merkleRoot);
    }
}
