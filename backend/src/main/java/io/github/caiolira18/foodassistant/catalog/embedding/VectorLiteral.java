package io.github.caiolira18.foodassistant.catalog.embedding;

public final class VectorLiteral {

    private VectorLiteral() {
    }

    public static String of(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
