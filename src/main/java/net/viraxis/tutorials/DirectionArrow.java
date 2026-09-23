package net.viraxis.tutorials;

final class DirectionArrow {
    private static final double MIN_LENGTH_SQUARED = 1.0E-6D;
    private static final double VERTICAL_THRESHOLD_RADIANS = Math.PI / 8.0D;

    private DirectionArrow() {}

    static String between(double viewX, double viewY, double viewZ,
                          double targetX, double targetY, double targetZ) {
        double viewLength = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        double targetLength = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
        if (viewLength * viewLength < MIN_LENGTH_SQUARED || targetLength * targetLength < MIN_LENGTH_SQUARED)
            return "↑";

        viewX /= viewLength;
        viewY /= viewLength;
        viewZ /= viewLength;
        targetX /= targetLength;
        targetY /= targetLength;
        targetZ /= targetLength;

        double pitchDifference = Math.asin(Math.max(-1.0D, Math.min(1.0D, targetY)))
                - Math.asin(Math.max(-1.0D, Math.min(1.0D, viewY)));
        String horizontal = between(viewX, viewZ, targetX, targetZ);
        if (pitchDifference <= -VERTICAL_THRESHOLD_RADIANS) {
            return switch (horizontal) {
                case "←", "↖", "↙" -> "↙";
                case "→", "↗", "↘" -> "↘";
                default -> "↓";
            };
        }
        if (pitchDifference >= VERTICAL_THRESHOLD_RADIANS) {
            return switch (horizontal) {
                case "←", "↖", "↙" -> "↖";
                case "→", "↗", "↘" -> "↗";
                default -> "↑";
            };
        }

        return horizontal;
    }

    static String between(double viewX, double viewZ, double targetX, double targetZ) {
        double viewLength = Math.hypot(viewX, viewZ);
        double targetLength = Math.hypot(targetX, targetZ);
        if (viewLength * viewLength < MIN_LENGTH_SQUARED || targetLength * targetLength < MIN_LENGTH_SQUARED)
            return "↑";

        viewX /= viewLength;
        viewZ /= viewLength;
        targetX /= targetLength;
        targetZ /= targetLength;
        double dot = Math.max(-1.0D, Math.min(1.0D, viewX * targetX + viewZ * targetZ));
        double cross = viewX * targetZ - viewZ * targetX;
        int direction = (int) Math.round(Math.atan2(cross, dot) / (Math.PI / 4.0D));
        return switch (direction) {
            case 1 -> "↗";
            case 2 -> "→";
            case 3 -> "↘";
            case 4, -4 -> "↓";
            case -3 -> "↙";
            case -2 -> "←";
            case -1 -> "↖";
            default -> "↑";
        };
    }
}
